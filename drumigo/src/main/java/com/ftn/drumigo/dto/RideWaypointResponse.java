package com.ftn.drumigo.dto;

import java.math.BigDecimal;

public record RideWaypointResponse(
    Long id,
    String address,
    BigDecimal lat,
    BigDecimal lng,
    Integer order
) {}

