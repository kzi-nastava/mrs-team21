package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideInconsistency;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.PanicEventResponse;
import com.ftn.drumigo.dto.PassengerResponse;
import com.ftn.drumigo.dto.ReviewResponse;
import com.ftn.drumigo.dto.ActiveRideIdResponse;
import com.ftn.drumigo.dto.RideCreateRequest;
import com.ftn.drumigo.dto.RideDetailsResponse;
import com.ftn.drumigo.dto.VehicleLocationUpdateRequest;
import com.ftn.drumigo.dto.RideInconsistencyCreateRequest;
import com.ftn.drumigo.dto.RideInconsistencyResponse;
import com.ftn.drumigo.dto.RideTrackingResponse;
import com.ftn.drumigo.dto.RideWaypointResponse;
import com.ftn.drumigo.dto.ride.request.RideCancelByDriverRequest;
import com.ftn.drumigo.dto.ride.request.RideStopRequest;
import com.ftn.drumigo.dto.ride.response.RideResponse;
import com.ftn.drumigo.mapper.DriverMapper;
import com.ftn.drumigo.mapper.PanicEventMapper;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.mapper.RideInconsistencyMapper;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.mapper.VehicleMapper;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.MapService;
import com.ftn.drumigo.service.RideService;
import com.ftn.drumigo.service.RideTrackingSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {
    
    private final RideService rideService;
    private final MapService mapService;
    private final RideMapper rideMapper;
    private final RideInconsistencyMapper rideInconsistencyMapper;
    private final ReviewMapper reviewMapper;
    private final PanicEventMapper panicEventMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final UserRepository userRepository;
    private final RideTrackingSimulationService rideTrackingSimulationService;


    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> createRide(
            @AuthenticationPrincipal CustomUserDetails passengerDetails,
            @Valid @RequestBody RideCreateRequest request) {
        Ride ride = rideService.create(passengerDetails.getUserId(), request);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.status(201).body(rideMapper.toResponse(ride, waypoints));
    }

    
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> startRide(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails driverDetails) {
        Ride ride = rideService.startRide(id, driverDetails.getUserId());
        rideTrackingSimulationService.startSimulation(id);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.ok(rideMapper.toResponse(ride, waypoints));
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<RideResponse>> getActiveRides() {
        List<Ride> rides = rideService.getActiveRides();
        List<RideResponse> responses = rides.stream()
            .map(ride -> {
                List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
                return rideMapper.toResponse(ride, waypoints);
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Returns the current user's active ride id for tracking (passenger: PENDING/ACCEPTED/ACTIVE; driver: ACCEPTED/ACTIVE).
     * Future scheduled rides are not returned until their scheduled start time.
     * Returns 404 if the user has no active ride.
     */
    @GetMapping("/me/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActiveRideIdResponse> getMyActiveRide(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return rideService.getMyActiveRide(
                userDetails.getUserId(),
                userDetails.getRole())
            .map(ride -> ResponseEntity.ok(new ActiveRideIdResponse(ride.getId())))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RideTrackingResponse> getRide(@PathVariable Long id) {
        Ride ride = rideService.getById(id);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);

        Integer overrideDurationSec = null;
        Instant overrideArrivalAt = null;
        if (ride.getStatus() == RideStatus.ACTIVE
                && ride.getVehicle() != null
                && ride.getVehicle().getCurrentLat() != null
                && ride.getVehicle().getCurrentLng() != null
                && waypoints != null
                && waypoints.size() >= 2) {
            RideWaypoint destinationWaypoint = waypoints.get(waypoints.size() - 1);
            if (destinationWaypoint.getLocation() != null
                    && destinationWaypoint.getLocation().getLat() != null
                    && destinationWaypoint.getLocation().getLng() != null) {
                Optional<Integer> remainingSec = mapService.getRemainingDurationSeconds(
                        ride.getVehicle().getCurrentLat().doubleValue(),
                        ride.getVehicle().getCurrentLng().doubleValue(),
                        destinationWaypoint.getLocation().getLat().doubleValue(),
                        destinationWaypoint.getLocation().getLng().doubleValue());
                if (remainingSec.isPresent()) {
                    overrideDurationSec = remainingSec.get();
                    overrideArrivalAt = Instant.now().plusSeconds(overrideDurationSec);
                }
            }
        }

        return ResponseEntity.ok(rideMapper.toTrackingResponse(ride, waypoints, overrideDurationSec, overrideArrivalAt));
    }

    /**
     * Start simulated vehicle movement for this ride (demo/E2E).
     * Ride must be ACTIVE with driver and at least 2 waypoints.
     */
    /**
     * Sync the displayed (e.g. capped) vehicle position back to the backend so backend and client stay aligned.
     * Allowed for the ride's driver or any passenger. Call after computing display position so next poll returns it.
     */
    @PutMapping("/{id}/tracking-position")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateTrackingPosition(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VehicleLocationUpdateRequest request) {
        rideService.updateTrackingPosition(id, userDetails.getUserId(), userDetails.getRole(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tracking-demo/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> startTrackingDemo(@PathVariable Long id) {
        rideTrackingSimulationService.startSimulation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Stop simulated vehicle movement for this ride.
     */
    @PostMapping("/{id}/tracking-demo/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> stopTrackingDemo(@PathVariable Long id) {
        rideTrackingSimulationService.stopSimulation(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/inconsistencies")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideInconsistencyResponse> createInconsistency(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RideInconsistencyCreateRequest request) {
        RideInconsistency inconsistency = rideService.createInconsistency(id, userDetails.getUserId(), request.note());
        return ResponseEntity.status(201).body(rideInconsistencyMapper.toResponse(inconsistency));
    }
    
    @GetMapping("/{id}/inconsistencies")
    public ResponseEntity<List<RideInconsistencyResponse>> getRideInconsistencies(@PathVariable Long id) {
        List<RideInconsistency> inconsistencies = rideService.getRideInconsistencies(id);
        List<RideInconsistencyResponse> responses = inconsistencies.stream()
            .map(rideInconsistencyMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}/end")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> endRide(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Ride ride = rideService.endRideByDriverId(id, userDetails.getUserId());
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.ok(rideMapper.toResponse(ride, waypoints));
    }
    
    @PutMapping("/{id}/cancel-by-driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> cancelByDriver(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RideCancelByDriverRequest request) {
        rideService.cancelByDriver(id, userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/cancel-by-passenger")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<Void> cancelByPassenger(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        rideService.cancelByPassenger(id, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/stop")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> stopRide(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RideStopRequest request) {
        Ride ride = rideService.stopRide(id, userDetails.getUserId(), request);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.ok(rideMapper.toResponse(ride, waypoints));
    }
    
    @GetMapping("/admin/rides/search")
    public ResponseEntity<List<RideResponse>> searchRidesByDriverName(
            @RequestParam String name) {
        List<Ride> rides = rideService.searchByDriverName(name);
        List<RideResponse> responses = rides.stream()
            .map(ride -> {
                List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
                return rideMapper.toResponse(ride, waypoints);
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}/details")
    public ResponseEntity<RideDetailsResponse> getRideDetails(@PathVariable Long id) {
        Ride ride = rideService.getById(id);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        
        // Get related data
        List<com.ftn.drumigo.domain.Review> reviews = rideService.getRideReviews(id);
        List<com.ftn.drumigo.domain.PanicEvent> panicEvents = rideService.getRidePanicEvents(id);
        List<com.ftn.drumigo.domain.RidePassenger> ridePassengers = rideService.getRidePassengers(ride);
        
        // Map to DTOs
        List<PassengerResponse> passengers = ridePassengers.stream()
            .map(rp -> {
                // Try to find the user by email if they're registered
                User user = userRepository.findByEmail(rp.getPassengerEmail()).orElse(null);
                if (user != null) {
                    return new PassengerResponse(
                        user.getId(),
                        user.getName(),
                        user.getSurname(),
                        user.getEmail()
                    );
                } else {
                    // If user not registered, use email as name
                    return new PassengerResponse(
                        null,
                        rp.getPassengerEmail(),
                        "(Unregistered)",
                        rp.getPassengerEmail()
                    );
                }
            })
            .collect(Collectors.toList());
        
        List<ReviewResponse> reviewResponses = reviews.stream()
            .map(reviewMapper::toResponse)
            .collect(Collectors.toList());
        
        List<PanicEventResponse> panicResponses = panicEvents.stream()
            .map(panicEventMapper::toResponse)
            .collect(Collectors.toList());
        
        List<RideInconsistencyResponse> inconsistencies = rideService.getRideInconsistencies(id).stream()
            .map(rideInconsistencyMapper::toResponse)
            .collect(Collectors.toList());
        
        List<RideWaypointResponse> waypointResponses = waypoints.stream()
            .map(wp -> new RideWaypointResponse(
                wp.getId(),
                wp.getLocation().getAddress(),
                wp.getLocation().getLat(),
                wp.getLocation().getLng(),
                wp.getWaypointOrder()
            ))
            .collect(Collectors.toList());
        
        RideDetailsResponse response = new RideDetailsResponse(
            rideMapper.toResponse(ride, waypoints),
            waypointResponses,
            ride.getDriver() != null ? driverMapper.toResponse(ride.getDriver()) : null,
            ride.getVehicle() != null ? vehicleMapper.toResponse(ride.getVehicle()) : null,
            passengers,
            inconsistencies,
            reviewResponses,
            panicResponses
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> reorderRide(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails passengerDetails) {
        Ride ride = rideService.reorderRide(id, passengerDetails.getUserId());
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.status(201).body(rideMapper.toResponse(ride, waypoints));
    }
}
