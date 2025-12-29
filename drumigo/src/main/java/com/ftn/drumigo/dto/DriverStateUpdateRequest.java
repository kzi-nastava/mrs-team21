package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotNull;

public record DriverStateUpdateRequest(
    @NotNull(message = "Active driver state is required")
    Boolean activeDriver
) {}

