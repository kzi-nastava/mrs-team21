package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReportChartResponse(
    Instant from,
    Instant to,
    List<ReportDayDto> dailyData,
    long totalRides,
    long totalDistanceKm,
    BigDecimal totalCost,
    double avgRidesPerDay,
    double avgDistanceKmPerDay,
    BigDecimal avgCostPerDay
) {}
