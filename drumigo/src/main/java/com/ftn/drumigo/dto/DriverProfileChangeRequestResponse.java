package com.ftn.drumigo.dto;

import com.ftn.drumigo.domain.enums.RequestStatus;
import java.time.Instant;

public record DriverProfileChangeRequestResponse(
    Long id,
    Long driverId,
    String driverName,
    String driverSurname,
    String requestedChangesJson,
    RequestStatus status,
    Instant createdAt,
    Instant reviewedAt,
    Long reviewedByAdminId
) {}

