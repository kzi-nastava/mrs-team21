package com.ftn.drumigo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FavoriteRouteCreateRequest(
    @NotNull(message = "Vehicle type ID is required")
    @Positive(message = "Vehicle type ID must be positive")
    Long vehicleTypeId,
    
    Boolean babyTransport,
    Boolean petTransport,
    
    @NotEmpty(message = "At least one waypoint is required")
    @Valid
    List<WaypointRequest> waypoints
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

