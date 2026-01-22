package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RideResponse(
    Long id,
    String status,
    Instant requestedAt,
    Instant scheduledFor,
    Instant startTime,
    Instant endTime,
    Long driverId,
    String driverName,
    String driverSurname,
    Long vehicleId,
    BigDecimal totalCost,
    BigDecimal totalDistanceKm,
    Integer estimatedDurationSec,
    Instant estimatedArrivalAt,
    Boolean babyTransport,
    Boolean petTransport,
    List<WaypointInfo> waypoints
) {
    public record WaypointInfo(
        Long locationId,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        Integer order
    ) {}
}

