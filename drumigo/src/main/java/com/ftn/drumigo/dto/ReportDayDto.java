package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportDayDto(
    LocalDate date,
    long rideCount,
    long distanceKm,
    BigDecimal cost
) {}
