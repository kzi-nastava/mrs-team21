package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.dto.history.response.PassengerRideHistoryItemResponse;
import com.ftn.drumigo.dto.history.request.RideHistoryRequest;
import com.ftn.drumigo.dto.UserResponse;
import com.ftn.drumigo.dto.auth.request.PassengerRegisterRequest;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.mapper.UserMapper;
import com.ftn.drumigo.service.PassengerService;
import com.ftn.drumigo.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {
    
    private final PassengerService passengerService;
    private final RideService rideService;
    private final RideMapper rideMapper;
    private final UserMapper userMapper;
    
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody PassengerRegisterRequest request) {
        Passenger passenger = passengerService.register(request);
        return ResponseEntity.status(201).body(userMapper.toResponse(passenger));
    }
    
    @GetMapping("/activate/{token}")
    public ResponseEntity<Void> activate(@PathVariable String token) {
        passengerService.activate(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{passengerId}/rides/history")
    public ResponseEntity<Page<PassengerRideHistoryItemResponse>> getPassengerRideHistory(
            @PathVariable Long passengerId,
            @ModelAttribute RideHistoryRequest request) {

        Pageable pageable = request.toPageable();
        List<RideStatus> statuses = request.parseStatuses();

        Page<Ride> rides = passengerService.getPassengerRideHistory(
                passengerId,
                request.getFrom(),
                request.getTo(),
                statuses,
                request.getHasPanic(),
                pageable
        );

        Page<PassengerRideHistoryItemResponse> responses = rides.map(ride -> {
            List<RideWaypoint> waypoints = rideService.getRideWaypoints(ride);
            boolean hasPanicForRide = rideService.hasPanic(ride.getId());
            return rideMapper.toPassengerHistoryResponse(ride, waypoints, hasPanicForRide);
        });

        return ResponseEntity.ok(responses);
    }
}
