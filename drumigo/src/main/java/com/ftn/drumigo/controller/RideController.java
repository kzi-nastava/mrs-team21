package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideInconsistency;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.*;
import com.ftn.drumigo.dto.PassengerResponse;
import com.ftn.drumigo.dto.ride.request.RideCancelByDriverRequest;
import com.ftn.drumigo.mapper.*;
import com.ftn.drumigo.dto.RideCreateRequest;
import com.ftn.drumigo.dto.RideInconsistencyCreateRequest;
import com.ftn.drumigo.dto.RideInconsistencyResponse;
import com.ftn.drumigo.dto.RideResponse;
import com.ftn.drumigo.dto.RideTrackingResponse;
import com.ftn.drumigo.mapper.RideInconsistencyMapper;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {
    
    private final RideService rideService;
    private final RideMapper rideMapper;
    private final RideInconsistencyMapper rideInconsistencyMapper;
    private final ReviewMapper reviewMapper;
    private final PanicEventMapper panicEventMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;


    @PostMapping
    public ResponseEntity<RideResponse> createRide(
            @RequestParam Long orderingPassengerId,
            @Valid @RequestBody RideCreateRequest request) {
        Ride ride = rideService.create(orderingPassengerId, request);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.status(201).body(rideMapper.toResponse(ride, waypoints));
    }

    
    @PutMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(
            @PathVariable Long id,
            @RequestParam Long driverId) {
        Ride ride = rideService.startRide(id, driverId);
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
    
    @GetMapping("/{id}")
    public ResponseEntity<RideTrackingResponse> getRide(@PathVariable Long id) {
        Ride ride = rideService.getById(id);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.ok(rideMapper.toTrackingResponse(ride, waypoints));
    }
    
    @PostMapping("/{id}/inconsistencies")
    public ResponseEntity<RideInconsistencyResponse> createInconsistency(
            @PathVariable Long id,
            @Valid @RequestBody RideInconsistencyCreateRequest request) {
        RideInconsistency inconsistency = rideService.createInconsistency(id, request);
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
    public ResponseEntity<RideResponse> endRide(@PathVariable Long id) {
        Ride ride = rideService.endRide(id);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.ok(rideMapper.toResponse(ride, waypoints));
    }
    
    @PutMapping("/{id}/cancel-by-driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> cancelByDriver(
            @PathVariable Long id,
            Principal principal,
            @Valid @RequestBody RideCancelByDriverRequest request) {
        rideService.cancelByDriver(id, principal.getName(), request);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/cancel-by-passenger")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<Void> cancelByPassenger(
            @PathVariable Long id,
            Principal principal) {
        rideService.cancelByPassenger(id, principal.getName());
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/stop")
    public ResponseEntity<RideResponse> stopRide(
            @PathVariable Long id,
            @RequestParam Long driverId,
            @Valid @RequestBody RideStopRequest request) {
        Ride ride = rideService.stopRide(id, driverId, request);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.ok(rideMapper.toResponse(ride, waypoints));
    }
    
    @GetMapping("/passengers/{passengerId}/rides/history")
    public ResponseEntity<Page<PassengerRideHistoryItemResponse>> getPassengerRideHistory(
            @PathVariable Long passengerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean hasPanic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt,desc") String sort) {
        
        List<RideStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            try {
                statuses = Arrays.stream(status.split(","))
                    .map(RideStatus::valueOf)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new com.ftn.drumigo.exception.BadRequestException("Invalid status value. Valid values: " + 
                    Arrays.toString(RideStatus.values()));
            }
        }
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Ride> rides = rideService.getPassengerRideHistory(passengerId, from, to, statuses, hasPanic, pageable);
        Page<PassengerRideHistoryItemResponse> responses = rides.map(ride -> {
            List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
            boolean hasPanicForRide = rideService.hasPanic(ride.getId());
            return rideMapper.toPassengerHistoryResponse(ride, waypoints, hasPanicForRide);
        });
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/admin/rides/history")
    public ResponseEntity<Page<RideResponse>> getAdminRideHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean hasPanic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt,desc") String sort) {
        
        List<RideStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            try {
                statuses = Arrays.stream(status.split(","))
                    .map(RideStatus::valueOf)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new com.ftn.drumigo.exception.BadRequestException("Invalid status value. Valid values: " + 
                    Arrays.toString(RideStatus.values()));
            }
        }
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Ride> rides = rideService.getAdminRideHistory(from, to, statuses, hasPanic, pageable);
        Page<RideResponse> responses = rides.map(ride -> {
            List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
            return rideMapper.toResponse(ride, waypoints);
        });
        
        return ResponseEntity.ok(responses);
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
            .map(rp -> new PassengerResponse(
                rp.getPassenger().getId(),
                rp.getPassenger().getName(),
                rp.getPassenger().getSurname(),
                rp.getPassenger().getEmail()
            ))
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
    public ResponseEntity<RideResponse> reorderRide(
            @PathVariable Long id,
            @RequestParam Long passengerId) {
        Ride ride = rideService.reorderRide(id, passengerId);
        List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
        return ResponseEntity.status(201).body(rideMapper.toResponse(ride, waypoints));
    }
}
