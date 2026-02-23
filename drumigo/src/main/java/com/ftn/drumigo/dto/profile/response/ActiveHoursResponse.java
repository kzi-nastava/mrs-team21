package com.ftn.drumigo.dto.profile.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hours worked by a driver in the last 24 hours (for 8h daily limit).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveHoursResponse {
    /** Hours worked in the last 24 hours. */
    private double hoursWorked;
    /** Maximum allowed hours in 24h (always 8). */
    private int maxHours = 8;
}
