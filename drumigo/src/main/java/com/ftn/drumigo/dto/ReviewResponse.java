package com.ftn.drumigo.dto;

import java.time.Instant;

public record ReviewResponse(
    Long id,
    Long rideId,
    Long passengerId,
    String passengerName,
    String passengerSurname,
    Integer ratingDriver,
    Integer ratingVehicle,
    String comment,
    Instant createdAt
) {}

