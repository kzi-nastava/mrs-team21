package com.ftn.drumigo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VehicleUpdateRequest(
    Long vehicleTypeId,
    
    String model,
    
    String licensePlate,
    
    @Min(value = 1, message = "Number of seats must be at least 1")
    Integer numSeats,
    
    Boolean babyFriendly,
    
    Boolean petFriendly,
    
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    BigDecimal currentLat,
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    BigDecimal currentLng,
    
    Boolean available
) {}

