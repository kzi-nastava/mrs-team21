package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.map.LocationDTO;
import com.ftn.drumigo.dto.VehicleLocationUpdateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.repository.RideWaypointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates driver location updates along the ride route for demo/E2E testing.
 * When started for a ride, a scheduled task periodically updates the assigned vehicle's
 * currentLat/currentLng so the frontend can display movement by polling GET /rides/{id}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideTrackingSimulationService {

    private static final int TICK_INTERVAL_MS = 3000;
    private static final double DEFAULT_SIM_DURATION_SEC = 120.0;
    private static final double MIN_METERS_PER_TICK = 5.0;
    /** Cap movement per tick so backend position never jumps more than mobile's display cap (40m); keeps backend and shown position aligned. */
    private static final double MAX_METERS_PER_TICK = 40.0;
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private final RideService rideService;
    private final VehicleService vehicleService;
    private final RideWaypointRepository rideWaypointRepository;
    private final MapService mapService;

    private final ConcurrentHashMap<Long, SimulationState> simulatedRides = new ConcurrentHashMap<>();

    /**
     * Start simulating vehicle movement for the given ride.
     * Ride must be ACTIVE, have a driver with a vehicle, and at least 2 waypoints.
     */
    public void startSimulation(Long rideId) {
        Ride ride = rideService.getById(rideId);
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new BadRequestException("Ride must be ACTIVE to start tracking demo. Current status: " + ride.getStatus());
        }
        if (ride.getDriver() == null || ride.getVehicle() == null) {
            throw new BadRequestException("Ride must have an assigned driver and vehicle.");
        }
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideWithLocationOrderByWaypointOrderAsc(ride);
        if (waypoints == null || waypoints.size() < 2) {
            throw new BadRequestException("Ride must have at least 2 waypoints for tracking demo.");
        }

        List<RoutePoint> routePoints = resolveRoutePoints(waypoints);
        List<Double> cumulativeDistances = buildCumulativeDistances(routePoints);
        double totalDistanceMeters = cumulativeDistances.get(cumulativeDistances.size() - 1);
        double metersPerTick = calculateMetersPerTick(totalDistanceMeters, ride.getEstimatedDurationSec());

        SimulationState newState = new SimulationState(
            routePoints,
            cumulativeDistances,
            totalDistanceMeters,
            0.0,
            metersPerTick
        );

        updateVehicleLocation(ride, BigDecimal.valueOf(routePoints.get(0).lat()), BigDecimal.valueOf(routePoints.get(0).lng()));
        SimulationState previousState = simulatedRides.put(rideId, newState);
        if (previousState == null) {
            log.info("Started tracking demo for ride {} ({} route points)", rideId, routePoints.size());
        } else {
            log.info("Restarted tracking demo for ride {} ({} route points)", rideId, routePoints.size());
        }
    }

    /**
     * Stop simulating vehicle movement for the given ride.
     */
    public void stopSimulation(Long rideId) {
        if (simulatedRides.remove(rideId) != null) {
            log.info("Stopped tracking demo for ride {}", rideId);
        }
    }

    @Scheduled(fixedRate = TICK_INTERVAL_MS)
    public void tick() {
        if (simulatedRides.isEmpty()) {
            return;
        }
        simulatedRides.forEach((rideId, state) -> {
            try {
                advanceSimulation(rideId, state);
            } catch (Exception e) {
                log.warn("Simulation tick failed for ride {}: {}", rideId, e.getMessage());
                simulatedRides.remove(rideId);
            }
        });
    }

    private void advanceSimulation(Long rideId, SimulationState state) {
        Ride ride = rideService.getById(rideId);
        if (ride.getStatus() != RideStatus.ACTIVE) {
            simulatedRides.remove(rideId);
            return;
        }
        if (state.routePoints() == null || state.routePoints().size() < 2) {
            simulatedRides.remove(rideId);
            return;
        }

        double baseDistanceMeters = state.distanceTraveledMeters();
        if (ride.getVehicle() != null && ride.getVehicle().getCurrentLat() != null && ride.getVehicle().getCurrentLng() != null) {
            double fromVehicle = distanceAlongRouteFromPosition(
                ride.getVehicle().getCurrentLat().doubleValue(),
                ride.getVehicle().getCurrentLng().doubleValue(),
                state.routePoints(),
                state.cumulativeDistancesMeters()
            );
            baseDistanceMeters = Math.max(baseDistanceMeters, fromVehicle);
        }

        double nextDistanceMeters = Math.min(
            state.totalDistanceMeters(),
            baseDistanceMeters + state.metersPerTick()
        );
        RoutePoint nextPoint = resolvePointAtDistance(state.routePoints(), state.cumulativeDistancesMeters(), nextDistanceMeters);
        updateVehicleLocation(ride, BigDecimal.valueOf(nextPoint.lat()), BigDecimal.valueOf(nextPoint.lng()));

        if (nextDistanceMeters >= state.totalDistanceMeters()) {
            simulatedRides.remove(rideId);
            log.info("Tracking demo finished for ride {} (reached destination)", rideId);
            return;
        }

        simulatedRides.put(rideId, state.withDistanceTraveled(nextDistanceMeters));
    }

    private double distanceAlongRouteFromPosition(double lat, double lng, List<RoutePoint> points, List<Double> cumulativeDistances) {
        if (points == null || points.size() < 2 || cumulativeDistances == null || cumulativeDistances.size() < 2) {
            return 0.0;
        }
        double minDistSq = Double.POSITIVE_INFINITY;
        double closestDistance = 0.0;
        double refLatRad = Math.toRadians(lat);
        double cosLat = Math.cos(refLatRad);
        if (Math.abs(cosLat) < 1e-9) cosLat = 1e-9;
        for (int i = 0; i < points.size() - 1; i++) {
            RoutePoint a = points.get(i);
            RoutePoint b = points.get(i + 1);
            double ax = Math.toRadians(a.lng()) * EARTH_RADIUS_M * cosLat;
            double ay = Math.toRadians(a.lat()) * EARTH_RADIUS_M;
            double bx = Math.toRadians(b.lng()) * EARTH_RADIUS_M * cosLat;
            double by = Math.toRadians(b.lat()) * EARTH_RADIUS_M;
            double px = Math.toRadians(lng) * EARTH_RADIUS_M * cosLat;
            double py = Math.toRadians(lat) * EARTH_RADIUS_M;
            double abx = bx - ax;
            double aby = by - ay;
            double apx = px - ax;
            double apy = py - ay;
            double abLenSq = abx * abx + aby * aby;
            double t = abLenSq <= 0 ? 0 : (apx * abx + apy * aby) / abLenSq;
            double clampedT = Math.max(0.0, Math.min(1.0, t));
            double projX = ax + abx * clampedT;
            double projY = ay + aby * clampedT;
            double dx = px - projX;
            double dy = py - projY;
            double distSq = dx * dx + dy * dy;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                double segStart = cumulativeDistances.get(i);
                double segEnd = cumulativeDistances.get(i + 1);
                closestDistance = segStart + (segEnd - segStart) * clampedT;
            }
        }
        return closestDistance;
    }

    private void updateVehicleLocation(Ride ride, BigDecimal lat, BigDecimal lng) {
        Long driverUserId = ride.getDriver().getId();
        vehicleService.updateCurrentDriverLocation(driverUserId, new VehicleLocationUpdateRequest(lat, lng));
    }

    private List<RoutePoint> resolveRoutePoints(List<RideWaypoint> waypoints) {
        List<LocationDTO> orderedWaypoints = waypoints.stream()
            .map(waypoint -> {
                Location location = waypoint.getLocation();
                return new LocationDTO(
                    location.getLat().doubleValue(),
                    location.getLng().doubleValue(),
                    location.getAddress()
                );
            })
            .toList();

        try {
            List<List<Double>> routeCoordinates = mapService.getRouteCoordinatesForOrderedWaypoints(orderedWaypoints);
            List<RoutePoint> points = routeCoordinates.stream()
                .filter(coord -> coord != null && coord.size() >= 2)
                .map(coord -> new RoutePoint(coord.get(1), coord.get(0)))
                .toList();
            if (points.size() >= 2) {
                return points;
            }
            log.warn("Mapbox returned insufficient route coordinates, falling back to straight waypoint path");
        } catch (Exception ex) {
            log.warn("Failed to load Mapbox route geometry for simulation, using waypoint fallback: {}", ex.getMessage());
        }

        return waypoints.stream()
            .map(waypoint -> new RoutePoint(
                waypoint.getLocation().getLat().doubleValue(),
                waypoint.getLocation().getLng().doubleValue()
            ))
            .toList();
    }

    private List<Double> buildCumulativeDistances(List<RoutePoint> points) {
        if (points == null || points.size() < 2) {
            throw new BadRequestException("Simulation route must contain at least 2 points.");
        }
        List<Double> cumulative = new ArrayList<>();
        cumulative.add(0.0);
        double runningDistance = 0.0;
        for (int i = 1; i < points.size(); i++) {
            runningDistance += distanceMeters(points.get(i - 1), points.get(i));
            cumulative.add(runningDistance);
        }
        return cumulative;
    }

    private RoutePoint resolvePointAtDistance(List<RoutePoint> points, List<Double> cumulativeDistances, double targetDistanceMeters) {
        if (targetDistanceMeters <= 0) {
            return points.get(0);
        }
        double total = cumulativeDistances.get(cumulativeDistances.size() - 1);
        if (targetDistanceMeters >= total) {
            return points.get(points.size() - 1);
        }

        for (int i = 1; i < cumulativeDistances.size(); i++) {
            double segmentEndDistance = cumulativeDistances.get(i);
            if (targetDistanceMeters > segmentEndDistance) {
                continue;
            }
            double segmentStartDistance = cumulativeDistances.get(i - 1);
            double segmentLength = segmentEndDistance - segmentStartDistance;
            double ratio = segmentLength <= 0 ? 0.0 : (targetDistanceMeters - segmentStartDistance) / segmentLength;
            RoutePoint from = points.get(i - 1);
            RoutePoint to = points.get(i);
            double lat = from.lat() + (to.lat() - from.lat()) * ratio;
            double lng = from.lng() + (to.lng() - from.lng()) * ratio;
            return new RoutePoint(lat, lng);
        }

        return points.get(points.size() - 1);
    }

    private double calculateMetersPerTick(double totalDistanceMeters, Integer estimatedDurationSec) {
        if (totalDistanceMeters <= 0) {
            return 0;
        }
        double durationSec = estimatedDurationSec != null && estimatedDurationSec > 0
            ? estimatedDurationSec
            : DEFAULT_SIM_DURATION_SEC;
        double ticks = Math.max(1.0, (durationSec * 1000.0) / TICK_INTERVAL_MS);
        double rawMetersPerTick = totalDistanceMeters / ticks;
        return Math.min(MAX_METERS_PER_TICK, Math.max(MIN_METERS_PER_TICK, rawMetersPerTick));
    }

    private double distanceMeters(RoutePoint from, RoutePoint to) {
        double lat1 = Math.toRadians(from.lat());
        double lat2 = Math.toRadians(to.lat());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(to.lng() - from.lng());
        double sinLat = Math.sin(dLat / 2);
        double sinLng = Math.sin(dLng / 2);
        double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    private record RoutePoint(double lat, double lng) {}

    private record SimulationState(
        List<RoutePoint> routePoints,
        List<Double> cumulativeDistancesMeters,
        double totalDistanceMeters,
        double distanceTraveledMeters,
        double metersPerTick
    ) {
        private SimulationState withDistanceTraveled(double nextDistanceTraveledMeters) {
            return new SimulationState(
                routePoints,
                cumulativeDistancesMeters,
                totalDistanceMeters,
                nextDistanceTraveledMeters,
                metersPerTick
            );
        }
    }
}
