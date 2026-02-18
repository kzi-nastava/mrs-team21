package com.ftn.drumigo.dto;

/**
 * Response for GET /api/rides/me/active.
 * Contains the current user's active ride id (for passengers: PENDING/ACCEPTED/ACTIVE; for drivers: ACCEPTED/ACTIVE).
 */
public record ActiveRideIdResponse(long rideId) {}
