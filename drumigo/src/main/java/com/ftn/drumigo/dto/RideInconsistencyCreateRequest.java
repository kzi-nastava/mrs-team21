package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RideInconsistencyCreateRequest(
    @NotNull(message = "Passenger ID is required")
    Long passengerId,
    
    @NotBlank(message = "Note is required")
    String note
) {}

