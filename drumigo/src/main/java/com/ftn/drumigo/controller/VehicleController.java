package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.dto.VehicleCreateRequest;
import com.ftn.drumigo.dto.VehicleLocationUpdateRequest;
import com.ftn.drumigo.dto.VehicleResponse;
import com.ftn.drumigo.dto.VehicleUpdateRequest;
import com.ftn.drumigo.mapper.VehicleMapper;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    
    private final VehicleService vehicleService;
    private final VehicleMapper vehicleMapper;
    
    @GetMapping("/active")
    public ResponseEntity<List<VehicleResponse>> getActiveVehicles() {
        return ResponseEntity.ok(vehicleService.getActiveVehicleResponses());
    }
    
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAll();
        List<VehicleResponse> responses = vehicles.stream()
            .map(vehicleMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getById(id);
        return ResponseEntity.ok(vehicleMapper.toResponse(vehicle));
    }
    
    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleCreateRequest request) {
        Vehicle vehicle = vehicleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleMapper.toResponse(vehicle));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Long id, 
                                                          @Valid @RequestBody VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleService.update(id, request);
        return ResponseEntity.ok(vehicleMapper.toResponse(vehicle));
    }
    
    @PutMapping("/{id}/location")
    public ResponseEntity<VehicleResponse> updateVehicleLocation(@PathVariable Long id,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                  @Valid @RequestBody VehicleLocationUpdateRequest request) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Vehicle vehicle = vehicleService.updateLocation(id, request, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(vehicleMapper.toResponse(vehicle));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

