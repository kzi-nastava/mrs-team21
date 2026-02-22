package com.drumigo.mobile.util;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Human-readable labels for notification types. Matches frontend NOTIFICATION_TYPE_LABELS.
 */
public final class NotificationTypeLabels {

    private static final Map<String, String> LABELS = new HashMap<>();

    static {
        LABELS.put("RIDE_ACCEPTED", "Ride accepted");
        LABELS.put("RIDE_REJECTED", "Ride rejected");
        LABELS.put("RIDE_STARTED", "Ride started");
        LABELS.put("RIDE_FINISHED", "Ride finished");
        LABELS.put("RIDE_CANCELLED", "Ride cancelled");
        LABELS.put("LINKED_TO_RIDE", "Linked to ride");
        LABELS.put("PANIC_ALERT", "Panic alert");
        LABELS.put("SUPPORT_MESSAGE", "Support message");
        LABELS.put("SCHEDULED_RIDE_REMINDER", "Scheduled ride reminder");
    }

    private NotificationTypeLabels() {
    }

    @NonNull
    public static String getLabel(@NonNull String type) {
        String label = LABELS.get(type);
        return label != null ? label : type;
    }
}
