package com.ftn.drumigo.dto;

import com.ftn.drumigo.domain.enums.VehicleTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public record RideCreateRequest(
    @NotEmpty(message = "At least one waypoint is required")
    @Valid
    List<WaypointRequest> waypoints,
    
    @NotNull(message = "Vehicle type is required")
    VehicleTypeName vehicleType,
    
    Boolean babyTransport,
    Boolean petTransport,
    
    List<String> linkedPassengerEmails,
    
    Instant scheduledFor
) {
    public record WaypointRequest(
        @NotBlank(message = "Address is required")
        String address,
        
        @NotNull(message = "Latitude is required")
        java.math.BigDecimal lat,
        
        @NotNull(message = "Longitude is required")
        java.math.BigDecimal lng,
        
        @NotNull(message = "Waypoint order is required")
        @Positive(message = "Waypoint order must be positive")
        Integer order
    ) {}
}

