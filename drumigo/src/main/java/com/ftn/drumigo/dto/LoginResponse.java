package com.ftn.drumigo.dto;

import com.ftn.drumigo.domain.enums.UserRole;

public record LoginResponse(
    Long userId,
    String email,
    UserRole role,
    String token
) {}

