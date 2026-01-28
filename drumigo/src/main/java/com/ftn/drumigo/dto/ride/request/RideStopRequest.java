package com.ftn.drumigo.dto.ride.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RideStopRequest(
    @NotBlank(message = "Stop address is required")
    String stopAddress,
    
    @NotNull(message = "Stop latitude is required")
    BigDecimal stopLat,
    
    @NotNull(message = "Stop longitude is required")
    BigDecimal stopLng
) {}

