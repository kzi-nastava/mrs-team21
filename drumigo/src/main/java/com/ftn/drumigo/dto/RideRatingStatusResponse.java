package com.ftn.drumigo.dto;

import java.time.Instant;

/**
 * Response DTO for ride rating eligibility status.
 * Used to determine if a passenger can rate a ride and show deadline information.
 */
public record RideRatingStatusResponse(
    boolean canRate,
    boolean hasReview,
    int daysRemaining,
    Instant ratingDeadline
) {}
