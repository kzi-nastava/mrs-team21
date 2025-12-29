package com.ftn.drumigo.dto;

import java.time.Instant;

public record PanicEventResponse(
    Long id,
    Long rideId,
    Long userId,
    String userEmail,
    String reason,
    Instant createdAt
) {}

