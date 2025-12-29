package com.ftn.drumigo.dto;

import java.time.Instant;

public record UserNoteResponse(
    Long id,
    Long userId,
    Long adminId,
    String adminName,
    String note,
    Instant createdAt
) {}

