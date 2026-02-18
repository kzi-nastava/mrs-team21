package com.ftn.drumigo.dto.ride.response;

import java.util.List;

public record EstimateResponse(
    String routePolyline,
    /** Route line as [lng, lat] pairs for map drawing. Mapbox order. */
    List<List<Double>> routeCoordinates,
    Double distanceInKm,
    Integer durationInMinutes,
    Double estimatedPrice
) {}

