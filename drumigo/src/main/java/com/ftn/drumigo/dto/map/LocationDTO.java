package com.ftn.drumigo.dto.map;

import jakarta.validation.constraints.NotNull;

public record LocationDTO(
    @NotNull(message = "Latitude is required")
    Double latitude,

    @NotNull(message = "Longitude is required")
    Double longitude,

    String address
) {}
