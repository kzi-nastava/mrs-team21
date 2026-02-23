package com.drumigo.mobile.data.model;

/**
 * Matches backend NotificationResponse. Dates as ISO-8601 strings.
 */
public class NotificationResponse {
    public Long id;
    public Long userId;
    public Long rideId;
    public String type;
    public String message;
    public String createdAt;
    public String readAt;
    public Integer reminderMinutesBefore;
}
