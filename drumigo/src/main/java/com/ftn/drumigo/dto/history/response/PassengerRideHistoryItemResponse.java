package com.ftn.drumigo.dto.history.response;

import com.ftn.drumigo.domain.enums.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PassengerRideHistoryItemResponse(
    Long id,
    RideStatus status,
    Instant requestedAt,
    Instant scheduledFor,
    Instant startTime,
    Instant endTime,
    String startAddress,
    String destinationAddress,
    BigDecimal totalCost,
    Boolean canceled,
    String canceledBy,
    Boolean hasPanic
) {}

