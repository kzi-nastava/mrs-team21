package com.ftn.drumigo.dto;

import java.math.BigDecimal;
import java.util.Map;

public record EstimateResponse(
    Integer estimatedDurationMinutes,
    BigDecimal estimatedDistanceKm,
    Map<String, BigDecimal> estimatedCostByVehicleType,
    String routePolyline
) {}

