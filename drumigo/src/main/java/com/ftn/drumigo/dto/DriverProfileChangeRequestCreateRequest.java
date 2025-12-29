package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverProfileChangeRequestCreateRequest(
    @NotBlank(message = "Requested changes JSON is required")
    String requestedChangesJson
) {}

