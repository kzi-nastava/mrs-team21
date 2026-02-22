package com.drumigo.mobile.data.model.ride;

/**
 * Request body for POST /rides/{id}/reviews.
 * Both ratings required (1-5), comment optional (max 500 chars).
 */
public class ReviewCreateRequest {
    public int ratingDriver;
    public int ratingVehicle;
    public String comment;

    public ReviewCreateRequest() {
    }

    public ReviewCreateRequest(int ratingDriver, int ratingVehicle, String comment) {
        this.ratingDriver = ratingDriver;
        this.ratingVehicle = ratingVehicle;
        this.comment = comment;
    }
}
