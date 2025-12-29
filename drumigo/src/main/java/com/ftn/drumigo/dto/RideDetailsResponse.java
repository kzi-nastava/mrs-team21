package com.ftn.drumigo.dto;

import java.util.List;

public record RideDetailsResponse(
    RideResponse ride,
    List<RideWaypointResponse> waypoints,
    DriverResponse driver,
    VehicleResponse vehicle,
    List<PassengerResponse> passengers,
    List<RideInconsistencyResponse> inconsistencies,
    List<ReviewResponse> reviews,
    List<PanicEventResponse> panicEvents
) {}

