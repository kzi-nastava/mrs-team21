package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a ride inconsistency report.
 * The passenger ID is extracted from the authenticated user (JWT token),
 * so only the note is required in the request body.
 */
public record RideInconsistencyCreateRequest(
    @NotBlank(message = "Note is required")
    @Size(min = 10, max = 1000, message = "Note must be between 10 and 1000 characters")
    String note
) {}

