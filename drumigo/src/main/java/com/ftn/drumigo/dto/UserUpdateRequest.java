package com.ftn.drumigo.dto;

import jakarta.validation.constraints.Email;

public record UserUpdateRequest(
    String name,
    String surname,
    @Email(message = "Email must be valid")
    String email,
    String address,
    String phone,
    String profilePictureUrl
) {}

