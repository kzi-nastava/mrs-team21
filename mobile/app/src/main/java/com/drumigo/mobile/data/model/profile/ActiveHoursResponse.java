package com.drumigo.mobile.data.model.profile;

/**
 * Hours worked by a driver in the last 24 hours (for 8h daily limit).
 */
public class ActiveHoursResponse {
    public double hoursWorked;
    public int maxHours = 8;
}
