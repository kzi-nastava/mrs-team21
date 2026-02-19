package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportMessageCreateRequest(
    Long receiverId,
    
    @NotBlank(message = "Content is required")
    String content
) {}

