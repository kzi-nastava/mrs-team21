package com.ftn.drumigo.dto;

import java.time.Instant;

public record DriverDocumentResponse(
        Long id,
        Long driverId,
        String documentName,
        String documentUrl,
        Instant uploadedAt) {
}
