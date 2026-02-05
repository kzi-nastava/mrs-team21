package com.ftn.drumigo.dto;

import java.math.BigDecimal;

public record VehicleResponse(
    Long id,
    Long driverId,
    String driverName,
    String driverSurname,
    Long vehicleTypeId,
    String vehicleTypeName,
    Integer numSeats,
    Boolean babyFriendly,
    Boolean petFriendly,
    BigDecimal currentLat,
    BigDecimal currentLng,
    Boolean available
) {}

