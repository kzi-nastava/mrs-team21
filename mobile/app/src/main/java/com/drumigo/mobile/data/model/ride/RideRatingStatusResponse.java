package com.drumigo.mobile.data.model.ride;

/**
 * Response from GET /rides/{id}/rating-status.
 * Indicates whether the passenger can rate, has already reviewed, and deadline info.
 */
public class RideRatingStatusResponse {
    public boolean canRate;
    public boolean hasReview;
    public int daysRemaining;
    /** ISO-8601 instant, e.g. "2024-12-21T14:00:00Z" */
    public String ratingDeadline;

    public RideRatingStatusResponse() {
    }
}
