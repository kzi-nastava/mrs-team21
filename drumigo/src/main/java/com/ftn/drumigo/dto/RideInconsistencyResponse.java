package com.ftn.drumigo.dto;

import java.time.Instant;

public record RideInconsistencyResponse(
    Long id,
    Long rideId,
    Long passengerId,
    String passengerName,
    String passengerSurname,
    String note,
    Instant createdAt
) {}

