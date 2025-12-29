package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;

public record RideCancelByDriverRequest(
    @NotBlank(message = "Cancel reason is required")
    String reason
) {}

