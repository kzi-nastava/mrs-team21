package com.ftn.drumigo.dto;

import java.time.Instant;

public record DriverResponse(
    Long id,
    String name,
    String surname,
    String email,
    String address,
    String phone,
    String profilePictureUrl,
    Boolean blocked,
    Boolean active,
    String licenseNumber,
    Boolean activeDriver,
    Instant lastStateChangeAt,
    Instant createdAt,
    Instant updatedAt
) {}

