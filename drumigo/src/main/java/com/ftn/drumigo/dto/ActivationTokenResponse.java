package com.ftn.drumigo.dto;

import java.time.Instant;

public record ActivationTokenResponse(
    String token,
    Instant expiresAt
) {}

