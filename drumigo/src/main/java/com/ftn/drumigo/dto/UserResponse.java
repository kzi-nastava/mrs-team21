package com.ftn.drumigo.dto;

import com.ftn.drumigo.domain.enums.UserRole;
import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String surname,
    String email,
    String address,
    String phone,
    String profilePictureUrl,
    Boolean blocked,
    UserRole role,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {}

