package com.drumigo.mobile.ui.history;

import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.history.PassengerRideHistoryItemResponse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class PassengerRideHistoryMapper {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private PassengerRideHistoryMapper() {
    }

    public static Ride toRide(PassengerRideHistoryItemResponse item) {
        if (item == null) {
            return null;
        }

        ZonedDateTime start = parseDateTime(firstNonBlank(item.startTime, item.scheduledFor, item.requestedAt));
        ZonedDateTime end = parseDateTime(item.endTime);

        String dateLabel = start != null ? DATE_FORMAT.format(start) : "";
        String timeLabel = formatTimeRange(start, end);
        String origin = isBlank(item.startAddress) ? "Unknown pickup" : item.startAddress;
        String destination = isBlank(item.destinationAddress) ? "Unknown destination" : item.destinationAddress;

        String statusLabel = mapStatus(item);
        String cancellationLabel = mapCancellation(item);
        String amountLabel = mapAmount(item);

        Ride ride = new Ride(
            item.id,
            dateLabel,
            timeLabel,
            origin,
            destination,
            new String[0],
            0,
            statusLabel,
            cancellationLabel,
            amountLabel,
            item.hasPanic != null && item.hasPanic
        );
        ride.setSortTimestampEpochMs(start != null ? start.toInstant().toEpochMilli() : 0L);
        ride.setAmountValue(item.totalCost == null ? 0.0d : item.totalCost);
        ride.setFavorite(item.isFavorite != null && item.isFavorite);
        ride.setFavoriteRouteId(item.favoriteRouteId);
        return ride;
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

    private static String mapStatus(PassengerRideHistoryItemResponse item) {
        if (item == null) {
            return "Completed";
        }
        return RideStatusMapper.toDisplayStatus(item.status, item.canceled);
    }

    private static String mapCancellation(PassengerRideHistoryItemResponse item) {
        if (item == null || item.canceled == null || !item.canceled || isBlank(item.canceledBy)) {
            return null;
        }
        return "By " + item.canceledBy.trim();
    }

    private static String mapAmount(PassengerRideHistoryItemResponse item) {
        if (item == null || item.totalCost == null) {
            return null;
        }
        if (item.canceled != null && item.canceled) {
            return null;
        }
        DecimalFormat formatter = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.ENGLISH));
        return formatter.format(item.totalCost) + " RSD";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
