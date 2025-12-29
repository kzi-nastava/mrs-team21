package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RideTrackingResponse(
    Long id,
    String status,
    Instant requestedAt,
    Instant startTime,
    Long driverId,
    String driverName,
    String driverSurname,
    Long vehicleId,
    String vehicleModel,
    String vehicleLicensePlate,
    BigDecimal vehicleCurrentLat,
    BigDecimal vehicleCurrentLng,
    List<WaypointInfo> waypoints,
    Instant estimatedArrivalAt,
    Integer estimatedDurationSec,
    BigDecimal totalDistanceKm,
    Boolean babyTransport,
    Boolean petTransport
) {
    public record WaypointInfo(
        Long locationId,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        Integer order
    ) {}
}

