package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.VehicleLocationUpdateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.repository.RideWaypointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private static final double PROGRESS_PER_TICK = 0.12;
    private static final int TICK_INTERVAL_MS = 3000;

    private final RideService rideService;
    private final VehicleService vehicleService;
    private final RideWaypointRepository rideWaypointRepository;

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
        simulatedRides.put(rideId, new SimulationState(0, 0.0));
        log.info("Started tracking demo for ride {}", rideId);
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
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideWithLocationOrderByWaypointOrderAsc(ride);
        if (waypoints == null || waypoints.size() < 2) {
            simulatedRides.remove(rideId);
            return;
        }
        int segmentCount = waypoints.size() - 1;
        int step = state.currentStepIndex();
        double progress = state.progressInStep();

        progress += PROGRESS_PER_TICK;
        if (progress >= 1.0) {
            step++;
            progress = 0.0;
        }

        if (step >= segmentCount) {
            // Reached destination: set position to last waypoint and stop simulation
            Location last = waypoints.get(waypoints.size() - 1).getLocation();
            updateVehicleLocation(ride, last.getLat(), last.getLng());
            simulatedRides.remove(rideId);
            log.info("Tracking demo finished for ride {} (reached destination)", rideId);
            return;
        }

        // Interpolate between waypoints[step] and waypoints[step+1]
        Location from = waypoints.get(step).getLocation();
        Location to = waypoints.get(step + 1).getLocation();
        double lat = from.getLat().doubleValue() + (to.getLat().doubleValue() - from.getLat().doubleValue()) * progress;
        double lng = from.getLng().doubleValue() + (to.getLng().doubleValue() - from.getLng().doubleValue()) * progress;

        updateVehicleLocation(ride, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
        simulatedRides.put(rideId, new SimulationState(step, progress));
    }

    private void updateVehicleLocation(Ride ride, BigDecimal lat, BigDecimal lng) {
        Long driverUserId = ride.getDriver().getId();
        vehicleService.updateCurrentDriverLocation(driverUserId, new VehicleLocationUpdateRequest(lat, lng));
    }

    private record SimulationState(int currentStepIndex, double progressInStep) {}
}
