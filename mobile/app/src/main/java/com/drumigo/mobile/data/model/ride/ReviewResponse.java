package com.drumigo.mobile.data.model.ride;

/**
 * Response from POST /rides/{id}/reviews or GET /rides/{id}/reviews.
 */
public class ReviewResponse {
    public Long id;
    public Long rideId;
    public Long passengerId;
    public String passengerName;
    public String passengerSurname;
    public Integer ratingDriver;
    public Integer ratingVehicle;
    public String comment;
    /** ISO-8601 instant */
    public String createdAt;

    public ReviewResponse() {
    }
}
