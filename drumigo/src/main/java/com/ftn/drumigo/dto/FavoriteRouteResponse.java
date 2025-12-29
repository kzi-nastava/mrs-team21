package com.ftn.drumigo.dto;

import java.time.Instant;
import java.util.List;

public record FavoriteRouteResponse(
    Long id,
    Long passengerId,
    Long vehicleTypeId,
    String vehicleTypeName,
    Boolean babyTransport,
    Boolean petTransport,
    List<WaypointResponse> waypoints,
    Instant createdAt
) {
    public record WaypointResponse(
        Long id,
        Long locationId,
        String address,
        java.math.BigDecimal lat,
        java.math.BigDecimal lng,
        Integer order
    ) {}
}

