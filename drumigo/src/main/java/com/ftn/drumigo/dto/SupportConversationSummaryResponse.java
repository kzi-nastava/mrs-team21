package com.ftn.drumigo.dto;

import java.time.Instant;

public record SupportConversationSummaryResponse(
    Long userId,
    String userName,
    String userSurname,
    String lastMessage,
    Instant lastMessageAt,
    Long lastSenderId,
    String lastSenderRole
) {}
