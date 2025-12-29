package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;

public record UserNoteCreateRequest(
    @NotBlank(message = "Note is required")
    String note
) {}

