package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportMessageCreateRequest(
    @NotNull(message = "Receiver ID is required")
    Long receiverId,
    
    @NotBlank(message = "Content is required")
    String content
) {}

