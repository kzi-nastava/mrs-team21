package com.drumigo.mobile.ui.history;

import com.drumigo.mobile.R;

final class RideStatusPresentation {

    final String label;
    final int badgeBackgroundRes;
    final int textColorRes;
    final int iconRes;

    private RideStatusPresentation(String label, int badgeBackgroundRes, int textColorRes, int iconRes) {
        this.label = label;
        this.badgeBackgroundRes = badgeBackgroundRes;
        this.textColorRes = textColorRes;
        this.iconRes = iconRes;
    }

    static RideStatusPresentation from(String statusLabel) {
        String normalized = statusLabel == null ? "" : statusLabel.trim().toUpperCase();

        if ("CANCELLED".equals(normalized)) {
            return new RideStatusPresentation(
                "Cancelled",
                R.drawable.bg_status_cancelled,
                R.color.danger,
                R.drawable.ic_close_circle
            );
        }
        if ("REJECTED".equals(normalized)) {
            return new RideStatusPresentation(
                "Rejected",
                R.drawable.bg_status_cancelled,
                R.color.danger,
                R.drawable.ic_close_circle
            );
        }
        if ("IN PROGRESS".equals(normalized)) {
            return new RideStatusPresentation(
                "In progress",
                R.drawable.bg_status_in_progress,
                R.color.status_active,
                R.drawable.ic_car
            );
        }
        if ("SCHEDULED".equals(normalized)) {
            return new RideStatusPresentation(
                "Scheduled",
                R.drawable.bg_status_scheduled,
                R.color.status_scheduled,
                R.drawable.ic_calendar
            );
        }
        if ("PENDING".equals(normalized)) {
            return new RideStatusPresentation(
                "Pending",
                R.drawable.bg_status_pending,
                R.color.status_pending,
                R.drawable.ic_clock_white
            );
        }
        return new RideStatusPresentation(
            "Completed",
            R.drawable.bg_status_completed,
            R.color.success,
            R.drawable.ic_check
        );
    }
}
