package com.ftn.drumigo.dto;

/**
 * Response for GET /api/rides/me/active.
 * Contains the current user's active ride id (for passengers: PENDING/ACCEPTED/ACTIVE; for drivers: ACCEPTED/ACTIVE).
 * Future scheduled rides are excluded until scheduled start time.
 */
public record ActiveRideIdResponse(long rideId) {}
