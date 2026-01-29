package com.ftn.drumigo.dto.auth.response;

import com.ftn.drumigo.domain.enums.UserRole;

public record LoginResponse(
    Long userId,
    String email,
    UserRole role,
    String token
) {}

