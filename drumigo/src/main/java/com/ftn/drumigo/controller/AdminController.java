package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.dto.RideResponse;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final RideService rideService;
    private final RideMapper rideMapper;
    
    @GetMapping("/rides/search")
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
}

