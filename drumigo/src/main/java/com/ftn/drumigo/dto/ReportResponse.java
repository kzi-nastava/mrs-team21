package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportResponse(
    Instant from,
    Instant to,
    Long totalRides,
    Long totalDistanceKm,
    BigDecimal totalCost,
    Long totalPassengers
) {}

