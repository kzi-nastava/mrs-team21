package com.ftn.drumigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverDocumentCreateRequest(
    @NotBlank(message = "Document name is required")
    String documentName,
    
    @Size(max = 500, message = "Document URL must not exceed 500 characters")
    String documentUrl
) {}

