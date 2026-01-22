package com.ftn.drumigo.dto;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    Long userId,
    Long rideId,
    String type,
    String message,
    Instant createdAt,
    Instant readAt
) {}

