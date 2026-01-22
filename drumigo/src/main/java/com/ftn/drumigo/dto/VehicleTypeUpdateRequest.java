package com.ftn.drumigo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VehicleTypeUpdateRequest(
    @NotNull(message = "Start price is required")
    @DecimalMin(value = "0.0", message = "Start price must be non-negative")
    BigDecimal startPrice,
    
    @NotNull(message = "Price per km is required")
    @DecimalMin(value = "0.0", message = "Price per km must be non-negative")
    BigDecimal pricePerKm
) {}

