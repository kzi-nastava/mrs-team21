package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.ReportChartResponse;
import com.ftn.drumigo.dto.ReportDayDto;
import com.ftn.drumigo.dto.ReportResponse;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.DriverRepository;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final ZoneOffset REPORT_ZONE = ZoneOffset.UTC;

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final RidePassengerRepository ridePassengerRepository;
    private final DriverRepository driverRepository;

    public ReportResponse getUserReport(Long userId, Instant from, Instant to) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        final Instant fromFinal = from != null ? from : Instant.ofEpochMilli(0);
        final Instant toFinal = to != null ? to : Instant.now();

        // Get all rides for this user (as ordering passenger or linked passenger)
        Passenger passenger = passengerRepository.findById(userId)
                .orElse(null);

        if (passenger == null) {
            // User is not a passenger, return empty report
            return new ReportResponse(fromFinal, toFinal, 0L, 0L, BigDecimal.ZERO, 0L);
        }

        // Use database query to get rides efficiently
        // Note: Using inclusive boundaries (!isBefore and !isAfter)
        List<Ride> userRides = rideRepository.findByStatusAndUserAndRequestedAtBetween(
                RideStatus.FINISHED, userId, user.getEmail(), fromFinal, toFinal);

        long totalRides = userRides.size();
        long totalDistanceKm = userRides.stream()
                .mapToLong(ride -> ride.getTotalDistanceKm() != null ? ride.getTotalDistanceKm().longValue() : 0L)
                .sum();
        BigDecimal totalCost = userRides.stream()
                .map(ride -> ride.getTotalCost() != null ? ride.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalPassengers = userRides.stream()
                .mapToLong(ride -> {
                    long count = 1; // ordering passenger
                    List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
                    count += ridePassengers.size();
                    return count;
                })
                .sum();

        return new ReportResponse(fromFinal, toFinal, totalRides, totalDistanceKm, totalCost, totalPassengers);
    }

    public ReportResponse getAdminReport(Instant from, Instant to) {
        final Instant fromFinal = from != null ? from : Instant.ofEpochMilli(0);
        final Instant toFinal = to != null ? to : Instant.now();

        // Use database query with inclusive boundaries
        List<Ride> finishedRides = rideRepository.findByStatusAndRequestedAtBetween(
                RideStatus.FINISHED, fromFinal, toFinal);

        long totalRides = finishedRides.size();
        long totalDistanceKm = finishedRides.stream()
                .mapToLong(ride -> ride.getTotalDistanceKm() != null ? ride.getTotalDistanceKm().longValue() : 0L)
                .sum();
        BigDecimal totalCost = finishedRides.stream()
                .map(ride -> ride.getTotalCost() != null ? ride.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalPassengers = finishedRides.stream()
                .mapToLong(ride -> {
                    long count = 1; // ordering passenger
                    List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
                    count += ridePassengers.size();
                    return count;
                })
                .sum();

        return new ReportResponse(fromFinal, toFinal, totalRides, totalDistanceKm, totalCost, totalPassengers);
    }

    /**
     * Chart report for the current user: driver sees earned, passenger sees spent.
     * Admin-only users get an empty chart.
     */
    public ReportChartResponse getChartReportForUser(Long userId, Instant from, Instant to) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        final Instant fromFinal = from != null ? from : Instant.ofEpochMilli(0);
        final Instant toFinal = to != null ? to : Instant.now();

        List<Ride> rides;
        if (user instanceof Driver driver) {
            rides = rideRepository.findByDriverAndStatusAndEndTimeBetween(driver, RideStatus.FINISHED, fromFinal, toFinal);
        } else if (user instanceof Passenger passenger) {
            rides = rideRepository.findFinishedPassengerRidesByReportDateBetween(
                    RideStatus.FINISHED, passenger.getId(), user.getEmail(), fromFinal, toFinal);
        } else {
            rides = List.of();
        }
        return buildChartResponse(fromFinal, toFinal, rides);
    }

    /**
     * Admin chart report: all_drivers / all_passengers (same data, different label) or one user.
     */
    public ReportChartResponse getChartReportForAdmin(Instant from, Instant to, String scope, Long userId) {
        final Instant fromFinal = from != null ? from : Instant.ofEpochMilli(0);
        final Instant toFinal = to != null ? to : Instant.now();

        if ("user".equals(scope) && userId != null) {
            return getChartReportForUser(userId, fromFinal, toFinal);
        }

        List<Ride> rides = rideRepository.findFinishedRidesByReportDateBetween(RideStatus.FINISHED, fromFinal, toFinal);
        return buildChartResponse(fromFinal, toFinal, rides);
    }

    private ReportChartResponse buildChartResponse(Instant from, Instant to, List<Ride> rides) {
        LocalDate fromDay = from.atOffset(REPORT_ZONE).toLocalDate();
        LocalDate toDay = to.atOffset(REPORT_ZONE).toLocalDate();

        Map<LocalDate, DayAggregate> buckets = new LinkedHashMap<>();
        for (LocalDate d = fromDay; !d.isAfter(toDay); d = d.plusDays(1)) {
            buckets.put(d, new DayAggregate());
        }

        for (Ride ride : rides) {
            Instant reportInstant = ride.getEndTime() != null ? ride.getEndTime() : ride.getRequestedAt();
            LocalDate day = reportInstant.atOffset(REPORT_ZONE).toLocalDate();
            if (day.isBefore(fromDay) || day.isAfter(toDay)) {
                continue;
            }
            buckets.computeIfAbsent(day, k -> new DayAggregate()).add(ride);
        }

        List<ReportDayDto> dailyData = new ArrayList<>();
        long totalRides = 0;
        long totalDistanceKm = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (LocalDate d = fromDay; !d.isAfter(toDay); d = d.plusDays(1)) {
            DayAggregate agg = buckets.getOrDefault(d, new DayAggregate());
            long dayRides = agg.rideCount;
            long dayDistance = agg.distanceKm;
            BigDecimal dayCost = agg.cost == null ? BigDecimal.ZERO : agg.cost;
            // Logical rule: rides, distance and money must always be in sync. No day can have
            // rides without distance/cost or distance/cost without rides.
            if (dayRides > 0 && (dayDistance == 0 || dayCost.compareTo(BigDecimal.ZERO) <= 0)) {
                dayRides = 0;
                dayDistance = 0;
                dayCost = BigDecimal.ZERO;
            }
            if ((dayDistance > 0 || dayCost.compareTo(BigDecimal.ZERO) > 0) && dayRides == 0) {
                dayDistance = 0;
                dayCost = BigDecimal.ZERO;
            }
            dailyData.add(new ReportDayDto(d, dayRides, dayDistance, dayCost));
            totalRides += dayRides;
            totalDistanceKm += dayDistance;
            totalCost = totalCost.add(dayCost);
        }

        long daysCount = dailyData.size();
        double avgRidesPerDay = daysCount == 0 ? 0 : (double) totalRides / daysCount;
        double avgDistanceKmPerDay = daysCount == 0 ? 0 : (double) totalDistanceKm / daysCount;
        BigDecimal avgCostPerDay = daysCount == 0 ? BigDecimal.ZERO : totalCost.divide(BigDecimal.valueOf(daysCount), 2, RoundingMode.HALF_UP);

        return new ReportChartResponse(
                from, to, dailyData,
                totalRides, totalDistanceKm, totalCost,
                avgRidesPerDay, avgDistanceKmPerDay, avgCostPerDay);
    }

    private static class DayAggregate {
        long rideCount;
        long distanceKm;
        BigDecimal cost = BigDecimal.ZERO;

        void add(Ride ride) {
            rideCount++;
            distanceKm += ride.getTotalDistanceKm() != null ? ride.getTotalDistanceKm().longValue() : 0L;
            cost = cost.add(ride.getTotalCost() != null ? ride.getTotalCost() : BigDecimal.ZERO);
        }
    }
}
