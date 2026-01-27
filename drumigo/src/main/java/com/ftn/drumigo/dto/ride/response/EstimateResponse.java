package com.ftn.drumigo.dto.ride.response;

public record EstimateResponse(
    String routePolyline,
    Double distanceInKm,
    Integer durationInMinutes,
    Double estimatedPrice
) {}

