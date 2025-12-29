package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordConfirmRequest(
    @NotBlank(message = "Token is required")
    String token,
    
    @NotBlank(message = "New password is required")
    String newPassword
) {}

