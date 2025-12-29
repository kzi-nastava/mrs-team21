package com.ftn.drumigo.dto;

import java.math.BigDecimal;

public record VehicleTypeResponse(
    Long id,
    String name,
    BigDecimal startPrice,
    BigDecimal pricePerKm
) {}

