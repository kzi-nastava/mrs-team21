package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.domain.enums.VehicleTypeName;
import com.ftn.drumigo.dto.map.LocationDTO;
import com.ftn.drumigo.dto.ride.request.EstimateRequest;
import com.ftn.drumigo.dto.ride.response.EstimateResponse;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.repository.VehicleTypeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import retrofit2.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    @Value("${mapbox.api.key}")
    private String MAPBOX_API_KEY;

    private final VehicleTypeRepository vehicleTypeRepository;

    public EstimateResponse estimateRide(EstimateRequest request) {
        DirectionsRoute route = getDirectionsRoute(request);
        double distanceKm = route.distance() / 1000.0;
        VehicleTypeName vehicleTypeName = request.vehicleTypeName() != null
            ? request.vehicleTypeName()
            : VehicleTypeName.STANDARD;
        Double price = estimatePrice(distanceKm, vehicleTypeName);
        List<List<Double>> routeCoordinates = decodeRouteGeometryToCoordinates(route.geometry());

        return new EstimateResponse(
                route.geometry(),
                routeCoordinates,
                Math.round(distanceKm * 10) / 10.0,
                (int) (route.duration() / 60),
                price
        );
    }

    private DirectionsRoute getDirectionsRoute(EstimateRequest request) {
        Point start = Point.fromLngLat(request.startLocation().longitude(), request.startLocation().latitude());
        Point destination = Point.fromLngLat(request.destinationLocation().longitude(), request.destinationLocation().latitude());

        ArrayList<Point> waypoints = new ArrayList<>();
        if (request.waypoints() != null) {
            for (LocationDTO location : request.waypoints()) {
                waypoints.add(Point.fromLngLat(location.longitude(), location.latitude()));
            }
        }

        MapboxDirections client = MapboxDirections.builder()
            .accessToken(MAPBOX_API_KEY)
            .origin(start)
            .waypoints(waypoints)
            .destination(destination)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build();

        Response<DirectionsResponse> response;
        try {
            response = client.executeCall();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get directions from Mapbox", e);
        }

        if (!response.isSuccessful()) {
            throw new BadRequestException("Failed to get directions from Mapbox: HTTP " + response.code());
        }

        DirectionsResponse body = response.body();
        if (body == null) {
            throw new BadRequestException("Failed to get directions from Mapbox: empty response body");
        }

        if (body.routes().isEmpty()) {
            throw new BadRequestException("Failed to get directions from Mapbox: no routes returned");
        }

        return body.routes().get(0);
    }

    /**
     * Estimate price by vehicle type: base_price + distance_km * price_per_km (from DB).
     */
    public Double estimatePrice(Double distanceInKm, VehicleTypeName vehicleTypeName) {
        VehicleType vehicleType = vehicleTypeRepository.findByName(vehicleTypeName)
            .orElseThrow(() -> new BadRequestException("Vehicle type not found: " + vehicleTypeName));
        return vehicleType.getStartPrice()
            .add(vehicleType.getPricePerKm().multiply(java.math.BigDecimal.valueOf(distanceInKm)))
            .doubleValue();
    }

    /**
     * Decode route geometry (encoded polyline or GeoJSON-like string) to [lng, lat] coordinates.
     * Mapbox Directions with geometries=geojson returns a JSON object; with default polyline returns encoded string.
     */
    private List<List<Double>> decodeRouteGeometryToCoordinates(String geometry) {
        if (geometry == null || geometry.isBlank()) {
            return List.of();
        }
        String trimmed = geometry.trim();
        if (trimmed.startsWith("[")) {
            return parseGeoJsonCoordinates(trimmed);
        }
        if (trimmed.startsWith("{")) {
            int start = trimmed.indexOf("\"coordinates\"");
            if (start >= 0) {
                int arrayStart = trimmed.indexOf('[', start);
                int depth = 0;
                int end = arrayStart;
                for (int i = arrayStart; i < trimmed.length(); i++) {
                    char c = trimmed.charAt(i);
                    if (c == '[') depth++;
                    else if (c == ']') { depth--; if (depth == 0) { end = i + 1; break; } }
                }
                return parseGeoJsonCoordinates(trimmed.substring(arrayStart, end));
            }
        }
        return decodePolyline(trimmed);
    }

    private List<List<Double>> parseGeoJsonCoordinates(String arr) {
        List<List<Double>> out = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        boolean inLng = true;
        double lng = 0, lat = 0;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '[' && num.length() == 0) continue;
            if (c == ',' || c == ']') {
                if (num.length() > 0) {
                    double v = Double.parseDouble(num.toString().trim());
                    num.setLength(0);
                    if (inLng) { lng = v; inLng = false; }
                    else { lat = v; inLng = true; out.add(List.of(lng, lat)); }
                }
                if (c == ']' && out.size() > 0) break;
            } else if (c == '-' || c == '.' || (c >= '0' && c <= '9')) {
                num.append(c);
            }
        }
        return out;
    }

    /** Decode Google/Mapbox encoded polyline to [lng, lat] pairs. */
    private List<List<Double>> decodePolyline(String encoded) {
        List<List<Double>> out = new ArrayList<>();
        int idx = 0;
        int len = encoded.length();
        double lat = 0, lng = 0;
        while (idx < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(idx++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            double dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)) / 1e5;
            lat += dlat;
            shift = 0; result = 0;
            do {
                b = encoded.charAt(idx++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            double dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)) / 1e5;
            lng += dlng;
            out.add(List.of(lng, lat));
        }
        return out;
    }
}
