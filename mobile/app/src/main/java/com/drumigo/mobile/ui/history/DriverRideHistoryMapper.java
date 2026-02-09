package com.drumigo.mobile.ui.history;

import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.history.DriverRideHistoryItemResponse;
import com.drumigo.mobile.data.model.history.PassengerInfo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DriverRideHistoryMapper {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final int MAX_INITIALS = 3;

    private DriverRideHistoryMapper() {
    }

    public static Ride toRide(DriverRideHistoryItemResponse item) {
        if (item == null) {
            return null;
        }

        ZonedDateTime start = parseDateTime(item.startTime);
        ZonedDateTime end = parseDateTime(item.endTime);

        String dateLabel = start != null ? DATE_FORMAT.format(start) : "";
        String timeLabel = formatTimeRange(start, end);

        String origin = item.startLocation != null && item.startLocation.address != null
            ? item.startLocation.address
            : "Unknown pickup";
        String destination = item.endLocation != null && item.endLocation.address != null
            ? item.endLocation.address
            : "Unknown destination";

        List<PassengerInfo> passengers = item.passengers == null
            ? new ArrayList<>()
            : item.passengers;
        int passengerCount = passengers.size();
        String[] initials = buildPassengerInitials(passengers);

        String statusLabel = mapStatus(item);
        String cancelledBy = mapCancelledBy(item);
        String priceLabel = mapPrice(item);
        boolean hasPanic = item.panicOccurred != null && item.panicOccurred;

        return new Ride(
            item.id,
            dateLabel,
            timeLabel,
            origin,
            destination,
            initials,
            passengerCount,
            statusLabel,
            cancelledBy,
            priceLabel,
            hasPanic
        );
    }

    private static ZonedDateTime parseDateTime(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(iso).atZone(ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            return null;
        }
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

    private static String mapStatus(DriverRideHistoryItemResponse item) {
        if (item == null) {
            return "Completed";
        }
        String status = item.status == null ? "" : item.status.trim().toUpperCase(Locale.ENGLISH);
        boolean cancelled = item.cancelled != null && item.cancelled;
        if (cancelled || "CANCELLED".equals(status)) {
            return "Cancelled";
        }
        if ("COMPLETED".equals(status) || "FINISHED".equals(status)) {
            return "Completed";
        }
        return "Completed";
    }

    private static String mapCancelledBy(DriverRideHistoryItemResponse item) {
        if (item == null || item.cancelled == null || !item.cancelled) {
            return null;
        }
        String name = item.canceledByName == null ? "" : item.canceledByName.trim();
        String surname = item.canceledBySurname == null ? "" : item.canceledBySurname.trim();
        String fullName = (name + " " + surname).trim();
        if (!fullName.isEmpty()) {
            return "By " + fullName;
        }
        return null;
    }

    private static String mapPrice(DriverRideHistoryItemResponse item) {
        if (item == null || item.totalCost == null) {
            return "—";
        }
        if (item.cancelled != null && item.cancelled) {
            return "—";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat formatter = new DecimalFormat("0.##", symbols);
        return "+" + formatter.format(item.totalCost) + " RSD";
    }

    private static String[] buildPassengerInitials(List<PassengerInfo> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            return new String[0];
        }
        int total = passengers.size();
        int limit = total > MAX_INITIALS ? MAX_INITIALS - 1 : total;
        List<String> initials = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            PassengerInfo passenger = passengers.get(i);
            initials.add(buildInitials(passenger));
        }
        if (total > MAX_INITIALS) {
            initials.add("+" + (total - (MAX_INITIALS - 1)));
        }
        return initials.toArray(new String[0]);
    }

    private static String buildInitials(PassengerInfo passenger) {
        if (passenger == null) {
            return "";
        }
        String first = passenger.name == null ? "" : passenger.name.trim();
        String last = passenger.surname == null ? "" : passenger.surname.trim();
        StringBuilder builder = new StringBuilder();
        if (!first.isEmpty()) {
            builder.append(first.substring(0, 1).toUpperCase(Locale.ENGLISH));
        }
        if (!last.isEmpty()) {
            builder.append(last.substring(0, 1).toUpperCase(Locale.ENGLISH));
        }
        return builder.toString();
    }
}
