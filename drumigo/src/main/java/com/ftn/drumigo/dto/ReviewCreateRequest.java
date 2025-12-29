package com.ftn.drumigo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
    @NotNull(message = "Driver rating is required")
    @Min(value = 1, message = "Driver rating must be between 1 and 5")
    @Max(value = 5, message = "Driver rating must be between 1 and 5")
    Integer ratingDriver,
    
    @NotNull(message = "Vehicle rating is required")
    @Min(value = 1, message = "Vehicle rating must be between 1 and 5")
    @Max(value = 5, message = "Vehicle rating must be between 1 and 5")
    Integer ratingVehicle,
    
    String comment
) {}

