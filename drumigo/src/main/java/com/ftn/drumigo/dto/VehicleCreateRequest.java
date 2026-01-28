package com.ftn.drumigo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VehicleCreateRequest(
    @NotNull(message = "Driver ID is required")
    Long driverId,
    
    @NotNull(message = "Vehicle type ID is required")
    Long vehicleTypeId,
    
    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "Number of seats must be at least 1")
    Integer numSeats,
    
    @NotNull(message = "Baby friendly flag is required")
    Boolean babyFriendly,
    
    @NotNull(message = "Pet friendly flag is required")
    Boolean petFriendly,
    
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    BigDecimal currentLat,
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    BigDecimal currentLng
) {}

