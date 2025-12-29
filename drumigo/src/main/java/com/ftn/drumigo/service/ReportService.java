package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Passenger;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.ReportResponse;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final RidePassengerRepository ridePassengerRepository;

    public ReportResponse getUserReport(Long userId, Instant from, Instant to) {
        // Verify user exists
        userRepository.findById(userId)
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
                RideStatus.FINISHED, userId, fromFinal, toFinal);

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
}
