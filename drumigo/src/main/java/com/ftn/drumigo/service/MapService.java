package com.ftn.drumigo.service;

import com.ftn.drumigo.dto.map.LocationDTO;
import com.ftn.drumigo.dto.ride.request.EstimateRequest;
import com.ftn.drumigo.dto.ride.response.EstimateResponse;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    @Value("${mapbox.api.key}")
    private String MAPBOX_API_KEY;

    public EstimateResponse estimateRide(EstimateRequest request) {
        DirectionsRoute route = getDirectionsRoute(request);

        return new EstimateResponse(
                route.geometry(),
                Math.round((route.distance() / 1000.0) * 10) / 10.0,
                (int) (route.duration() / 60),
                estimatePrice(route.distance() / 1000)
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
            .build();

        Response<DirectionsResponse> response;
        try {
            response = client.executeCall();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to get directions from Mapbox: HTTP " + response.code());
        }

        DirectionsResponse body = response.body();
        if (body == null) {
            throw new RuntimeException("Failed to get directions from Mapbox: empty response body");
        }

        if (body.routes().isEmpty()) {
            throw new RuntimeException("Failed to get directions from Mapbox: no routes returned");
        }

        return body.routes().get(0);
    }

    public Double estimatePrice(Double distanceInKm) {
        //TODO: Implement base price based on the vehicle type
        int basePrice = 260;
        return basePrice + distanceInKm * 120;
    }
}
