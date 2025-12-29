package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EstimateRequest(
    @NotBlank(message = "Start address is required")
    String startAddress,
    
    @NotNull(message = "Start latitude is required")
    BigDecimal startLat,
    
    @NotNull(message = "Start longitude is required")
    BigDecimal startLng,
    
    @NotBlank(message = "Destination address is required")
    String destinationAddress,
    
    @NotNull(message = "Destination latitude is required")
    BigDecimal destinationLat,
    
    @NotNull(message = "Destination longitude is required")
    BigDecimal destinationLng
) {}

