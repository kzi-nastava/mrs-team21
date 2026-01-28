package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.dto.auth.request.PassengerRegisterRequest;
import com.ftn.drumigo.mapper.UserMapper;
import com.ftn.drumigo.dto.UserResponse;
import com.ftn.drumigo.service.PassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {
    
    private final PassengerService passengerService;
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
}

