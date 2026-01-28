package com.ftn.drumigo.dto.profile.request;

import jakarta.validation.constraints.Email;

public record ProfileUpdateRequest(
    String name,
    String surname,
    @Email(message = "Email must be valid")
    String email,
    String address,
    String phone,
    String profilePictureUrl
) {}
