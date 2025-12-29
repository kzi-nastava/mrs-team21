package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideInconsistency;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.dto.RideInconsistencyCreateRequest;
import com.ftn.drumigo.dto.RideInconsistencyResponse;
import com.ftn.drumigo.dto.RideResponse;
import com.ftn.drumigo.dto.RideTrackingResponse;
import com.ftn.drumigo.mapper.RideInconsistencyMapper;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {
    
    private final RideService rideService;
    private final RideMapper rideMapper;
    private final RideInconsistencyMapper rideInconsistencyMapper;
    
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
}

