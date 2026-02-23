package com.drumigo.mobile.data.model.panic;

/**
 * Matches backend PanicEventResponse. createdAt is ISO-8601 string.
 */
public class PanicEventResponse {
    public Long id;
    public Long rideId;
    public Long userId;
    public String userEmail;
    public String reason;
    public String createdAt;
}
