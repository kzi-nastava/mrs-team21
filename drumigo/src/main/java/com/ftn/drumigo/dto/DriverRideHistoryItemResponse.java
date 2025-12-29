package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DriverRideHistoryItemResponse(
    Long id,
    String status,
    Instant startTime,
    Instant endTime,
    LocationInfo startLocation,
    LocationInfo endLocation,
    Boolean cancelled,
    Long canceledByUserId,
    String canceledByName,
    String canceledBySurname,
    BigDecimal totalCost,
    List<PassengerInfo> passengers,
    Boolean panicOccurred
) {
    public record LocationInfo(
        String address,
        BigDecimal lat,
        BigDecimal lng
    ) {}
    
    public record PassengerInfo(
        Long id,
        String name,
        String surname
    ) {}
}

