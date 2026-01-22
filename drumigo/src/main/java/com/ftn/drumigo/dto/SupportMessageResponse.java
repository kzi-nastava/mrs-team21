package com.ftn.drumigo.dto;

import java.time.Instant;

public record SupportMessageResponse(
    Long id,
    Long senderId,
    String senderName,
    String senderSurname,
    Long receiverId,
    String receiverName,
    String receiverSurname,
    String content,
    Instant createdAt
) {}

