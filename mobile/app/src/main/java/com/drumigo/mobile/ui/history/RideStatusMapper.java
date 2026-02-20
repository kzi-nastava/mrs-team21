package com.drumigo.mobile.ui.history;

import java.util.Locale;

final class RideStatusMapper {

    private RideStatusMapper() {
    }

    static String toDisplayStatus(String backendStatus, Boolean cancelledFlag) {
        if (Boolean.TRUE.equals(cancelledFlag)) {
            return "Cancelled";
        }
        String normalized = normalize(backendStatus);
        if ("CANCELLED".equals(normalized)) {
            return "Cancelled";
        }
        if ("REJECTED".equals(normalized)) {
            return "Rejected";
        }
        if ("ACTIVE".equals(normalized) || "IN_PROGRESS".equals(normalized) || "IN PROGRESS".equals(normalized)) {
            return "In progress";
        }
        if ("ACCEPTED".equals(normalized) || "SCHEDULED".equals(normalized)) {
            return "Scheduled";
        }
        if ("PENDING".equals(normalized)) {
            return "Pending";
        }
        if ("FINISHED".equals(normalized) || "COMPLETED".equals(normalized)) {
            return "Completed";
        }
        return "Completed";
    }

    static String toDisplayStatus(String backendStatus) {
        return toDisplayStatus(backendStatus, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ENGLISH);
    }
}
