package com.drumigo.mobile.ui.history;

import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.ride.RideResponse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AdminRideHistoryMapper {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private AdminRideHistoryMapper() {
    }

    public static Ride toRide(RideResponse item) {
        if (item == null) {
            return null;
        }

        ZonedDateTime start = parseDateTime(firstNonBlank(item.startTime, item.scheduledFor, item.requestedAt));
        ZonedDateTime end = parseDateTime(item.endTime);
        String dateLabel = start != null ? DATE_FORMAT.format(start) : "";
        String timeLabel = formatTimeRange(start, end);

        List<RideResponse.WaypointInfo> waypoints = sortWaypoints(item.waypoints);
        String origin = "Unknown pickup";
        String destination = "Unknown destination";
        if (!waypoints.isEmpty()) {
            RideResponse.WaypointInfo first = waypoints.get(0);
            RideResponse.WaypointInfo last = waypoints.get(waypoints.size() - 1);
            if (first != null && !isBlank(first.address)) {
                origin = first.address;
            }
            if (last != null && !isBlank(last.address)) {
                destination = last.address;
            }
        }

        String statusLabel = mapStatus(item.status);
        String amountLabel = mapAmount(item, statusLabel);

        Ride ride = new Ride(
            item.id,
            dateLabel,
            timeLabel,
            origin,
            destination,
            new String[0],
            0,
            statusLabel,
            null,
            amountLabel,
            false
        );
        ride.setSortTimestampEpochMs(start != null ? start.toInstant().toEpochMilli() : 0L);
        ride.setAmountValue(item.totalCost == null ? 0.0d : item.totalCost);
        return ride;
    }

    private static List<RideResponse.WaypointInfo> sortWaypoints(List<RideResponse.WaypointInfo> input) {
        if (input == null) {
            return new ArrayList<>();
        }
        List<RideResponse.WaypointInfo> copy = new ArrayList<>(input);
        copy.sort(Comparator.comparingInt(item -> item == null || item.order == null ? Integer.MAX_VALUE : item.order));
        return copy;
    }

    private static ZonedDateTime parseDateTime(String iso) {
        if (isBlank(iso)) {
            return null;
        }
        try {
            return Instant.parse(iso).atZone(ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String formatTimeRange(ZonedDateTime start, ZonedDateTime end) {
        if (start == null) {
            return "";
        }
        String startLabel = TIME_FORMAT.format(start);
        if (end == null) {
            return startLabel;
        }
        return startLabel + " - " + TIME_FORMAT.format(end);
    }

    private static String mapStatus(String status) {
        return RideStatusMapper.toDisplayStatus(status);
    }

    private static String mapAmount(RideResponse item, String statusLabel) {
        if (item == null || item.totalCost == null || "Cancelled".equals(statusLabel)) {
            return null;
        }
        DecimalFormat formatter = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.ENGLISH));
        return formatter.format(item.totalCost) + " RSD";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
