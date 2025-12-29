package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.dto.VehicleTypeResponse;
import com.ftn.drumigo.dto.VehicleTypeUpdateRequest;
import com.ftn.drumigo.mapper.VehicleTypeMapper;
import com.ftn.drumigo.service.VehicleTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicle-types")
@RequiredArgsConstructor
public class VehicleTypeController {
    
    private final VehicleTypeService vehicleTypeService;
    private final VehicleTypeMapper vehicleTypeMapper;
    
    @GetMapping
    public ResponseEntity<List<VehicleTypeResponse>> getAllVehicleTypes() {
        List<VehicleType> types = vehicleTypeService.getAll();
        List<VehicleTypeResponse> responses = types.stream()
            .map(vehicleTypeMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<VehicleTypeResponse> getVehicleType(@PathVariable Long id) {
        VehicleType type = vehicleTypeService.getById(id);
        return ResponseEntity.ok(vehicleTypeMapper.toResponse(type));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<VehicleTypeResponse> updateVehicleType(@PathVariable Long id,
                                                                  @Valid @RequestBody VehicleTypeUpdateRequest request) {
        VehicleType type = vehicleTypeService.update(id, request);
        return ResponseEntity.ok(vehicleTypeMapper.toResponse(type));
    }
}

