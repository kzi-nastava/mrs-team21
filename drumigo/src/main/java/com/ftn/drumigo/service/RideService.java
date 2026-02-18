package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.*;
import com.ftn.drumigo.domain.enums.CancelReasonType;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.RideCreateRequest;
import com.ftn.drumigo.dto.ride.request.RideStopRequest;
import com.ftn.drumigo.dto.ride.request.RideCancelByDriverRequest;
import com.ftn.drumigo.event.RideFinishedEvent;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.*;
import com.ftn.drumigo.dto.map.LocationDTO;
import com.ftn.drumigo.dto.ride.request.EstimateRequest;
import com.ftn.drumigo.dto.ride.response.EstimateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RideService {
    
    private final RideRepository rideRepository;
    private final RideWaypointRepository rideWaypointRepository;
    private final RideInconsistencyRepository rideInconsistencyRepository;
    private final RidePassengerRepository ridePassengerRepository;
    private final PassengerRepository passengerRepository;
    private final NotificationRepository notificationRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final PanicEventRepository panicEventRepository;
    private final ReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MapService mapService;
    private final AssignmentNotificationService assignmentNotificationService;
    private static final String NO_ACTIVE_DRIVERS_MESSAGE = "There are currently no active drivers.";
    private static final long TEN_MINUTES_IN_SECONDS = Duration.ofMinutes(10).getSeconds();
    private static final long DEFAULT_RIDE_DURATION_SECONDS = Duration.ofMinutes(15).getSeconds();
    private static final List<RideStatus> ASSIGNMENT_BLOCKING_STATUSES = List.of(RideStatus.ACTIVE, RideStatus.ACCEPTED);

    public List<Ride> getActiveRides() {
        return rideRepository.findByStatus(RideStatus.ACTIVE);
    }

    /**
     * Returns the current user's "active" ride for tracking, if any.
     * Passenger: ride in PENDING, ACCEPTED, or ACTIVE (as ordering or linked passenger).
     * Driver: ride in ACCEPTED or ACTIVE assigned to them.
     */
    public Optional<Ride> getMyActiveRide(Long userId, String role) {
        if ("PASSENGER".equals(role)) {
            Optional<Passenger> passengerOpt = passengerRepository.findById(userId);
            if (passengerOpt.isEmpty()) {
                return Optional.empty();
            }
            Passenger passenger = passengerOpt.get();
            List<RideStatus> statuses = List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE);
            return rideRepository
                .findActiveRidesForPassenger(
                    passenger.getId(),
                    passenger.getEmail(),
                    statuses
                )
                .stream()
                .sorted(Comparator
                    .comparingInt((Ride r) -> switch (r.getStatus()) {
                        case ACTIVE -> 0;
                        case ACCEPTED -> 1;
                        default -> 2;
                    })
                    .thenComparing(Ride::getRequestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
        }
        if ("DRIVER".equals(role)) {
            Optional<Driver> driverOpt = driverRepository.findById(userId);
            if (driverOpt.isEmpty()) {
                return Optional.empty();
            }
            List<RideStatus> statuses = List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE);
            return rideRepository
                .findByDriverAndStatusIn(driverOpt.get(), statuses)
                .stream()
                .sorted(Comparator
                    .comparingInt((Ride r) -> r.getStatus() == RideStatus.ACTIVE ? 0 : 1)
                    .thenComparing(Ride::getRequestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
        }
        return Optional.empty();
    }
    
    public Ride getById(Long id) {
        return rideRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }
    
    public List<RideWaypoint> getRideWaypoints(Ride ride) {
        return rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
    }
    
    /**
     * Create a ride inconsistency report.
     * Security: Only passengers who are part of the ride can report inconsistencies.
     * 
     * @param rideId the ride ID
     * @param passengerId the ID of the authenticated passenger
     * @param note the inconsistency description
     * @return the created RideInconsistency
     * @throws ResourceNotFoundException if ride or passenger not found
     * @throws BadRequestException if passenger is not part of the ride
     */
    public RideInconsistency createInconsistency(Long rideId, Long passengerId, String note) {
        Ride ride = getById(rideId);
        
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with ID: " + passengerId));

        // Validate passenger is part of the ride (ordering passenger or linked passenger)
        boolean isOrderingPassenger = ride.getOrderingPassenger() != null 
            && ride.getOrderingPassenger().getId().equals(passenger.getId());
        
        boolean isLinkedPassenger = ridePassengerRepository.findByRide(ride).stream()
            .anyMatch(rp -> rp.getPassengerEmail().equals(passenger.getEmail()));

        if (!isOrderingPassenger && !isLinkedPassenger) {
            throw new BadRequestException("You are not authorized to report inconsistencies for this ride");
        }
        
        RideInconsistency inconsistency = new RideInconsistency();
        inconsistency.setRide(ride);
        inconsistency.setPassenger(passenger);
        inconsistency.setNote(note);
        
        return rideInconsistencyRepository.save(inconsistency);
    }
    
    public List<RideInconsistency> getRideInconsistencies(Long rideId) {
        Ride ride = getById(rideId);
        return rideInconsistencyRepository.findByRide(ride);
    }
    
    public List<Ride> searchByDriverName(String name) {
        return rideRepository.findByDriverNameContaining(name);
    }
    
    public Ride endRide(Long id) {
        Ride ride = getById(id);
        
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new BadRequestException("Ride must be ACTIVE to be ended. Current status: " + ride.getStatus());
        }
        
        ride.setStatus(RideStatus.FINISHED);
        ride.setEndTime(Instant.now());
        ride.setPaidAt(Instant.now());
        setDriverBusy(ride.getDriver(), false);
        
        ride = rideRepository.save(ride);
        eventPublisher.publishEvent(new RideFinishedEvent(ride.getId()));
        
        return ride;
    }

    /**
     * End a ride as the authenticated driver.
     * Security: only the driver assigned to the ride can end it.
     */
    public Ride endRideByDriverId(Long rideId, Long driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));

        Ride ride = getById(rideId);
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())) {
            throw new BadRequestException("Driver is not assigned to this ride");
        }

        return endRide(rideId);
    }
    
    public Page<Ride> getDriverRideHistory(Long driverId, Instant from, Instant to, Pageable pageable) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        
        if (from == null) {
            from = Instant.ofEpochMilli(0);
        }
        if (to == null) {
            to = Instant.now();
        }
        
        return rideRepository.findByDriverAndRequestedAtBetween(driver, from, to, pageable);
    }

    public Page<Ride> getUpcomingDriverRides(Long driverId, Instant from, Pageable pageable) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        if (from == null) {
            from = Instant.now();
        }

        return rideRepository.findByDriverAndStatusAndScheduledForAfter(
            driver,
            RideStatus.ACCEPTED,
            from,
            pageable
        );
    }
    
    public Ride create(Long orderingPassengerId, RideCreateRequest request) {
        Passenger orderingPassenger = passengerRepository.findById(orderingPassengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + orderingPassengerId));
        
        // Prevent creating new ride while having active ride (spec 2.6.1)
        List<RideStatus> activeStatuses = List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE);
        if (rideRepository.existsActiveRideForPassenger(orderingPassenger.getId(), orderingPassenger.getEmail(), activeStatuses)) {
            throw new BadRequestException("Cannot create a new ride while you have an active ride. Please wait until your current ride is finished.");
        }
        
        // Validate minimum waypoints (at least start and destination)
        if (request.waypoints() == null || request.waypoints().size() < 2) {
            throw new BadRequestException("Ride must have at least 2 waypoints (start and destination)");
        }
        
        // Validate scheduled_for (max 5 hours ahead)
        if (request.scheduledFor() != null) {
            Instant maxScheduled = Instant.now().plus(Duration.ofHours(5));
            if (request.scheduledFor().isAfter(maxScheduled)) {
                throw new BadRequestException("Ride cannot be scheduled more than 5 hours in advance");
            }
        }
        
        // Get vehicle type
        VehicleType vehicleType = vehicleTypeRepository.findByName(request.vehicleType())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + request.vehicleType()));
        
        // Create ride
        Ride ride = new Ride();
        ride.setStatus(RideStatus.PENDING);
        ride.setRequestedAt(Instant.now());
        ride.setScheduledFor(request.scheduledFor());
        ride.setOrderingPassenger(orderingPassenger);
        ride.setBabyTransport(request.babyTransport() != null ? request.babyTransport() : false);
        ride.setPetTransport(request.petTransport() != null ? request.petTransport() : false);
        
        // Snapshot pricing
        ride.setPricingStartPrice(vehicleType.getStartPrice());
        ride.setPricingPricePerKm(vehicleType.getPricePerKm());
        ride.setPricingVehicleTypeName(vehicleType.getName().name());

        // Distance, duration and cost from Mapbox Directions (single source of truth)
        EstimateRequest estimateRequest = buildEstimateRequestFromWaypoints(request);
        EstimateResponse estimate = mapService.estimateRide(estimateRequest);
        BigDecimal totalDistance = BigDecimal.valueOf(estimate.distanceInKm());
        ride.setTotalDistanceKm(totalDistance);
        ride.setTotalCost(BigDecimal.valueOf(estimate.estimatedPrice()));
        int estimatedSeconds = estimate.durationInMinutes() * 60;
        ride.setEstimatedDurationSec(estimatedSeconds);
        if (request.scheduledFor() != null) {
            ride.setEstimatedArrivalAt(request.scheduledFor().plusSeconds(estimatedSeconds));
        } else {
            ride.setEstimatedArrivalAt(Instant.now().plusSeconds(estimatedSeconds));
        }
        
        ride = rideRepository.save(ride);
        
        // Create waypoints
        for (RideCreateRequest.WaypointRequest wpRequest : request.waypoints()) {
            // Check if location already exists
            Location location = locationRepository.findByAddressAndLatAndLng(
                    wpRequest.address(), wpRequest.lat(), wpRequest.lng())
                    .orElse(null);
            
            if (location == null) {
                // Create new location only if it doesn't exist
                location = new Location();
                location.setAddress(wpRequest.address());
                location.setLat(wpRequest.lat());
                location.setLng(wpRequest.lng());
                location = locationRepository.save(location);
            }
            
            RideWaypoint waypoint = new RideWaypoint();
            waypoint.setRide(ride);
            waypoint.setLocation(location);
            waypoint.setWaypointOrder(wpRequest.order());
            
            rideWaypointRepository.save(waypoint);
        }
        
        // Assign driver according to the spec:
        // 1) nearest free driver, 2) fallback to busy driver finishing in <=10 minutes.
        RideCreateRequest.WaypointRequest pickupWaypoint = getPickupWaypoint(request.waypoints());
        DriverAssignmentResult assignmentResult = assignDriver(ride, vehicleType, pickupWaypoint);
        Driver assignedDriver = assignmentResult.driver();
        if (assignedDriver != null) {
            ride.setDriver(assignedDriver);
            Vehicle vehicle = vehicleRepository.findByDriver(assignedDriver)
                .orElse(null);
            if (vehicle != null) {
                ride.setVehicle(vehicle);
            }
            ride.setStatus(RideStatus.ACCEPTED);
            if (shouldMarkDriverBusyOnAssignment(ride)) {
                setDriverBusy(assignedDriver, true);
            }
            
            // Create notifications
            createAcceptNotifications(ride);
        } else {
            ride.setStatus(RideStatus.REJECTED);
            // Create rejection notification for ordering passenger
            createRejectionNotification(ride, assignmentResult.rejectionMessage());
        }
        
        ride = rideRepository.save(ride);
        
        // Process linked passengers: validate first, then handle based on assignment outcome
        List<String> linkedEmails = request.linkedPassengerEmails() != null ? request.linkedPassengerEmails() : List.of();
        for (String email : linkedEmails) {
            if (email.equals(orderingPassenger.getEmail())) {
                throw new BadRequestException("Cannot link the ordering passenger to their own ride");
            }
        }
        
        if (assignedDriver != null) {
            // ACCEPTED: add RidePassenger records and notify linked passengers (notification + email)
            for (String email : linkedEmails) {
                RidePassenger ridePassenger = new RidePassenger();
                ridePassenger.setRide(ride);
                ridePassenger.setPassengerEmail(email);
                ridePassengerRepository.save(ridePassenger);
            }
            assignmentNotificationService.notifyLinkedPassengersAccepted(ride, linkedEmails);
        } else {
            // REJECTED: notify linked passengers (no RidePassenger records)
            assignmentNotificationService.notifyLinkedPassengersRejected(
                    ride, assignmentResult.rejectionMessage(), linkedEmails);
        }
        
        return ride;
    }
    
    public Ride startRide(Long rideId, Long driverId) {
        Ride ride = getById(rideId);
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new BadRequestException("Driver is not assigned to this ride");
        }
        
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new BadRequestException("Ride must be ACCEPTED to be started. Current status: " + ride.getStatus());
        }
        
        // Check if driver has worked more than 8 hours in last 24 hours
        if (hasExceededWorkingHours(driver)) {
            throw new BadRequestException("Driver has exceeded 8 working hours in the last 24 hours");
        }
        
        ride.setStatus(RideStatus.ACTIVE);
        ride.setStartTime(Instant.now());
        setDriverBusy(driver, true);
        
        // Update estimated arrival based on start time
        if (ride.getEstimatedDurationSec() != null) {
            ride.setEstimatedArrivalAt(Instant.now().plusSeconds(ride.getEstimatedDurationSec()));
        }
        
        ride = rideRepository.save(ride);
        
        // Create notifications
        createStartNotifications(ride);
        
        return ride;
    }
    
    private EstimateRequest buildEstimateRequestFromWaypoints(RideCreateRequest request) {
        List<RideCreateRequest.WaypointRequest> ordered = new ArrayList<>(request.waypoints());
        ordered.sort(Comparator.comparingInt(RideCreateRequest.WaypointRequest::order));
        if (ordered.size() < 2) {
            throw new BadRequestException("Ride must have at least 2 waypoints (start and destination)");
        }
        LocationDTO startLocation = new LocationDTO(
            ordered.get(0).lat().doubleValue(),
            ordered.get(0).lng().doubleValue(),
            ordered.get(0).address()
        );
        LocationDTO destinationLocation = new LocationDTO(
            ordered.get(ordered.size() - 1).lat().doubleValue(),
            ordered.get(ordered.size() - 1).lng().doubleValue(),
            ordered.get(ordered.size() - 1).address()
        );
        List<LocationDTO> middleWaypoints = ordered.size() > 2
            ? ordered.subList(1, ordered.size() - 1).stream()
                .map(wp -> new LocationDTO(wp.lat().doubleValue(), wp.lng().doubleValue(), wp.address()))
                .toList()
            : null;
        return new EstimateRequest(startLocation, destinationLocation, middleWaypoints, request.vehicleType());
    }

    private RideCreateRequest.WaypointRequest getPickupWaypoint(List<RideCreateRequest.WaypointRequest> waypoints) {
        return waypoints.stream()
            .min(Comparator.comparingInt(RideCreateRequest.WaypointRequest::order))
            .orElseThrow(() -> new BadRequestException("Ride must have at least one pickup waypoint"));
    }
    
    private DriverAssignmentResult assignDriver(
            Ride ride,
            VehicleType vehicleType,
            RideCreateRequest.WaypointRequest pickupWaypoint
    ) {
        List<Driver> activeDrivers = driverRepository.findByActiveDriverTrue();

        if (activeDrivers.isEmpty()) {
            return DriverAssignmentResult.rejected(NO_ACTIVE_DRIVERS_MESSAGE);
        }

        Instant now = Instant.now();
        List<DriverAssignmentCandidate> eligibleCandidates = new ArrayList<>();

        for (Driver driver : activeDrivers) {
            if (hasExceededWorkingHours(driver)) {
                continue;
            }

            Vehicle vehicle = vehicleRepository.findByDriver(driver).orElse(null);
            if (!isVehicleEligibleForRide(ride, vehicleType, vehicle)) {
                continue;
            }

            List<Ride> driverAssignments = rideRepository.findByDriverAndStatusIn(driver, ASSIGNMENT_BLOCKING_STATUSES);
            double pickupDistanceKm = calculateDistanceToPickupKm(vehicle, pickupWaypoint);
            boolean hasFutureScheduledRide = driverAssignments.stream().anyMatch(assignment ->
                assignment.getStatus() == RideStatus.ACCEPTED
                    && assignment.getScheduledFor() != null
                    && assignment.getScheduledFor().isAfter(now)
            );
            Instant availableAt = estimateDriverAvailableAt(driverAssignments, now);
            long remainingToFinishSec = estimateRemainingToFinishSec(availableAt, now);
            boolean currentlyOccupied = remainingToFinishSec > 0;
            Instant assignmentStart = resolveAssignmentStart(ride, availableAt, now);
            boolean reservationConflict = hasReservationConflict(driverAssignments, ride, assignmentStart, now);

            eligibleCandidates.add(new DriverAssignmentCandidate(
                driver,
                pickupDistanceKm,
                hasFutureScheduledRide,
                currentlyOccupied,
                remainingToFinishSec,
                availableAt,
                reservationConflict
            ));
        }

        if (eligibleCandidates.isEmpty()) {
            return DriverAssignmentResult.rejected(NO_ACTIVE_DRIVERS_MESSAGE);
        }

        if (ride.getScheduledFor() != null) {
            return assignScheduledRideDriver(eligibleCandidates, ride.getScheduledFor());
        }

        return assignImmediateRideDriver(eligibleCandidates);
    }

    private DriverAssignmentResult assignScheduledRideDriver(
            List<DriverAssignmentCandidate> eligibleCandidates,
            Instant scheduledFor
    ) {
        List<DriverAssignmentCandidate> scheduledCandidates = eligibleCandidates.stream()
            .filter(candidate -> !candidate.reservationConflict())
            .filter(candidate -> !candidate.availableAt().isAfter(scheduledFor))
            .toList();

        if (scheduledCandidates.isEmpty()) {
            return DriverAssignmentResult.rejected(NO_ACTIVE_DRIVERS_MESSAGE);
        }

        Driver reservedDriver = scheduledCandidates.stream()
            .min(Comparator
                .comparingDouble(DriverAssignmentCandidate::pickupDistanceKm)
                .thenComparing(DriverAssignmentCandidate::availableAt)
                .thenComparing(candidate -> candidate.driver().getId()))
            .orElseThrow()
            .driver();

        return DriverAssignmentResult.assigned(reservedDriver);
    }

    private DriverAssignmentResult assignImmediateRideDriver(List<DriverAssignmentCandidate> eligibleCandidates) {
        List<DriverAssignmentCandidate> freeCandidates = eligibleCandidates.stream()
            .filter(candidate -> !candidate.currentlyOccupied())
            .filter(candidate -> !candidate.reservationConflict())
            .toList();

        if (!freeCandidates.isEmpty()) {
            Driver nearestFreeDriver = freeCandidates.stream()
                .min(Comparator
                    .comparingDouble(DriverAssignmentCandidate::pickupDistanceKm)
                    .thenComparing(candidate -> candidate.driver().getId()))
                .orElseThrow()
                .driver();

            return DriverAssignmentResult.assigned(nearestFreeDriver);
        }

        if (eligibleCandidates.stream().allMatch(
                candidate -> candidate.reservationConflict()
                    || (candidate.currentlyOccupied() && candidate.hasFutureScheduledRide())
        )) {
            return DriverAssignmentResult.rejected(NO_ACTIVE_DRIVERS_MESSAGE);
        }

        List<DriverAssignmentCandidate> fallbackCandidates = eligibleCandidates.stream()
            .filter(DriverAssignmentCandidate::currentlyOccupied)
            .filter(candidate -> !candidate.reservationConflict())
            .filter(candidate -> !candidate.hasFutureScheduledRide())
            .filter(candidate -> candidate.remainingToFinishSec() <= TEN_MINUTES_IN_SECONDS)
            .toList();

        if (fallbackCandidates.isEmpty()) {
            return DriverAssignmentResult.rejected(NO_ACTIVE_DRIVERS_MESSAGE);
        }

        Driver fallbackDriver = fallbackCandidates.stream()
            .min(Comparator
                .comparingLong(DriverAssignmentCandidate::remainingToFinishSec)
                .thenComparingDouble(DriverAssignmentCandidate::pickupDistanceKm)
                .thenComparing(candidate -> candidate.driver().getId()))
            .orElseThrow()
            .driver();

        return DriverAssignmentResult.assigned(fallbackDriver);
    }

    private boolean isVehicleEligibleForRide(Ride ride, VehicleType vehicleType, Vehicle vehicle) {
        if (vehicle == null || vehicle.getVehicleType() == null || vehicle.getVehicleType().getId() == null) {
            return false;
        }

        if (!vehicle.getVehicleType().getId().equals(vehicleType.getId())) {
            return false;
        }

        if (ride.getBabyTransport() && !Boolean.TRUE.equals(vehicle.getBabyFriendly())) {
            return false;
        }

        return !ride.getPetTransport() || Boolean.TRUE.equals(vehicle.getPetFriendly());
    }

    private double calculateDistanceToPickupKm(Vehicle vehicle, RideCreateRequest.WaypointRequest pickupWaypoint) {
        if (vehicle == null || vehicle.getCurrentLat() == null || vehicle.getCurrentLng() == null) {
            return Double.MAX_VALUE;
        }
        return haversineDistanceKm(
            vehicle.getCurrentLat().doubleValue(),
            vehicle.getCurrentLng().doubleValue(),
            pickupWaypoint.lat().doubleValue(),
            pickupWaypoint.lng().doubleValue()
        );
    }

    /** Approximate distance in km between two points (for driver-assignment comparison only, not pricing). */
    private static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latRad = Math.toRadians(lat2 - lat1);
        double lonRad = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latRad / 2) * Math.sin(latRad / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonRad / 2) * Math.sin(lonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Instant estimateDriverAvailableAt(List<Ride> assignments, Instant now) {
        Instant availableAt = now;
        for (Ride assignment : assignments) {
            if (assignment.getStatus() == RideStatus.ACCEPTED
                    && assignment.getScheduledFor() != null
                    && assignment.getScheduledFor().isAfter(now)) {
                // Future scheduled rides reserve a future slot, but do not block current availability.
                continue;
            }

            Instant startAt = estimateRideStart(assignment, now);
            Instant endAt = estimateRideEnd(assignment, startAt);
            if (endAt.isAfter(availableAt)) {
                availableAt = endAt;
            }
        }

        return availableAt;
    }

    private Instant resolveAssignmentStart(Ride ride, Instant availableAt, Instant now) {
        Instant requestedStart = ride.getScheduledFor() != null ? ride.getScheduledFor() : now;
        return requestedStart.isAfter(availableAt) ? requestedStart : availableAt;
    }

    private boolean hasReservationConflict(
            List<Ride> existingAssignments,
            Ride newRide,
            Instant assignmentStart,
            Instant now
    ) {
        Instant assignmentEnd = estimateRideEnd(newRide, assignmentStart);

        for (Ride existingAssignment : existingAssignments) {
            if (newRide.getId() != null && newRide.getId().equals(existingAssignment.getId())) {
                continue;
            }

            Instant existingStart = estimateRideStart(existingAssignment, now);
            Instant existingEnd = estimateRideEnd(existingAssignment, existingStart);
            if (intervalsOverlap(assignmentStart, assignmentEnd, existingStart, existingEnd)) {
                return true;
            }
        }

        return false;
    }

    private Instant estimateRideStart(Ride ride, Instant now) {
        if (ride.getStatus() == RideStatus.ACTIVE && ride.getStartTime() != null) {
            return ride.getStartTime();
        }
        if (ride.getScheduledFor() != null) {
            return ride.getScheduledFor();
        }
        if (ride.getStartTime() != null) {
            return ride.getStartTime();
        }
        if (ride.getRequestedAt() != null) {
            return ride.getRequestedAt();
        }
        return now;
    }

    private Instant estimateRideEnd(Ride ride, Instant startAt) {
        if (ride.getEndTime() != null) {
            return ride.getEndTime();
        }
        if (ride.getEstimatedArrivalAt() != null && !ride.getEstimatedArrivalAt().isBefore(startAt)) {
            return ride.getEstimatedArrivalAt();
        }
        return startAt.plusSeconds(estimateRideDurationSec(ride));
    }

    private long estimateRideDurationSec(Ride ride) {
        if (ride.getEstimatedDurationSec() != null && ride.getEstimatedDurationSec() > 0) {
            return ride.getEstimatedDurationSec();
        }
        return DEFAULT_RIDE_DURATION_SECONDS;
    }

    private boolean intervalsOverlap(Instant startA, Instant endA, Instant startB, Instant endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private long estimateRemainingToFinishSec(Instant availableAt, Instant now) {
        if (!availableAt.isAfter(now)) {
            return 0;
        }
        return Duration.between(now, availableAt).getSeconds();
    }

    private void setDriverBusy(Driver driver, boolean busy) {
        if (driver == null) {
            return;
        }
        driver.setBusy(busy);
        driverRepository.save(driver);
    }

    private boolean shouldMarkDriverBusyOnAssignment(Ride ride) {
        return ride.getScheduledFor() == null || !ride.getScheduledFor().isAfter(Instant.now());
    }
    
    private boolean hasExceededWorkingHours(Driver driver) {
        Instant now = Instant.now();
        Instant twentyFourHoursAgo = now.minus(Duration.ofHours(24));
        
        List<Ride> allRecentRides = rideRepository.findDriverRidesWithActivitySince(driver, twentyFourHoursAgo);
        
        long totalSeconds = 0;
        for (Ride ride : allRecentRides) {
            // Only count rides that were actually active (ACTIVE, FINISHED, or CANCELLED that had started)
            if (ride.getStartTime() == null) {
                continue;
            }
            
            if (ride.getStatus() == RideStatus.FINISHED && ride.getEndTime() != null) {
                // For finished rides, use actual duration
                Instant rideStart = ride.getStartTime();
                Instant rideEnd = ride.getEndTime();
                // Only count the portion within the 24-hour window
                Instant windowStart = rideStart.isBefore(twentyFourHoursAgo) ? twentyFourHoursAgo : rideStart;
                Instant windowEnd = rideEnd.isAfter(now) ? now : rideEnd;
                if (!windowStart.isAfter(windowEnd)) {
                    totalSeconds += Duration.between(windowStart, windowEnd).getSeconds();
                }
            } else if (ride.getStatus() == RideStatus.ACTIVE) {
                // For active rides, count from start time (or window start) to now
                Instant rideStart = ride.getStartTime();
                Instant windowStart = rideStart.isBefore(twentyFourHoursAgo) ? twentyFourHoursAgo : rideStart;
                totalSeconds += Duration.between(windowStart, now).getSeconds();
            } else if (ride.getStatus() == RideStatus.CANCELLED && ride.getEndTime() != null) {
                // For canceled rides that had started, count the time until cancellation
                Instant rideStart = ride.getStartTime();
                Instant rideEnd = ride.getEndTime();
                Instant windowStart = rideStart.isBefore(twentyFourHoursAgo) ? twentyFourHoursAgo : rideStart;
                Instant windowEnd = rideEnd.isAfter(now) ? now : rideEnd;
                if (!windowStart.isAfter(windowEnd)) {
                    totalSeconds += Duration.between(windowStart, windowEnd).getSeconds();
                }
            }
        }
        
        long eightHoursInSeconds = 8 * 60 * 60;
        return totalSeconds >= eightHoursInSeconds;
    }
    
    private void createAcceptNotifications(Ride ride) {
        // Notification for assigned driver
        if (ride.getDriver() != null) {
            Notification notification = new Notification();
            notification.setUser(ride.getDriver());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_ACCEPTED);
            notification.setMessage("You have been assigned a new ride");
            notificationRepository.save(notification);
        }

        // Notification for ordering passenger
        if (ride.getOrderingPassenger() != null) {
            Notification notification = new Notification();
            notification.setUser(ride.getOrderingPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_ACCEPTED);
            notification.setMessage("Your ride has been accepted");
            notificationRepository.save(notification);
        }
    }
    
    private void createStartNotifications(Ride ride) {
        // Notification for ordering passenger
        if (ride.getOrderingPassenger() != null) {
            Notification notification = new Notification();
            notification.setUser(ride.getOrderingPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_STARTED);
            notification.setMessage("Your ride has started");
            notificationRepository.save(notification);
        }
        
        // Notifications for linked passengers (only if registered)
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
        for (RidePassenger rp : ridePassengers) {
            User linkedUser = userRepository.findByEmail(rp.getPassengerEmail()).orElse(null);
            if (linkedUser != null) {
                Notification notification = new Notification();
                notification.setUser(linkedUser);
                notification.setRide(ride);
                notification.setType(NotificationType.RIDE_STARTED);
                notification.setMessage("Your ride has started");
                notificationRepository.save(notification);
            }
        }
    }
    
    private void createRejectionNotification(Ride ride, String reasonMessage) {
        // Notification for ordering passenger when ride is rejected
        if (ride.getOrderingPassenger() != null) {
            Notification notification = new Notification();
            notification.setUser(ride.getOrderingPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_REJECTED);
            notification.setMessage("Your ride request has been rejected. " + reasonMessage);
            notificationRepository.save(notification);
        }
    }

    private record DriverAssignmentCandidate(
        Driver driver,
        double pickupDistanceKm,
        boolean hasFutureScheduledRide,
        boolean currentlyOccupied,
        long remainingToFinishSec,
        Instant availableAt,
        boolean reservationConflict
    ) {
    }

    private record DriverAssignmentResult(Driver driver, String rejectionMessage) {
        private static DriverAssignmentResult assigned(Driver driver) {
            return new DriverAssignmentResult(driver, null);
        }

        private static DriverAssignmentResult rejected(String rejectionMessage) {
            return new DriverAssignmentResult(null, rejectionMessage);
        }
    }
    
    public void cancelByDriver(Long rideId, Long driverId, RideCancelByDriverRequest request) {
        Ride ride = getById(rideId);
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));

        // Cannot cancel if driver is not assigned to this ride
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new BadRequestException("Driver is not assigned to this ride");
        }
        
        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.ACTIVE) {
            throw new BadRequestException("Ride cannot be cancelled. Current status: " + ride.getStatus());
        }
        
        cancelRide(ride, request.cancelReasonType(), request.reason(), driver);
    }
    
    public void cancelByPassenger(Long rideId, Long passengerId) {
        Ride ride = getById(rideId);
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with ID: " + passengerId));

        // Check if passenger is the ordering passenger
        if (ride.getOrderingPassenger() == null || !ride.getOrderingPassenger().getId().equals(passengerId)) {
            throw new BadRequestException("Only the ordering passenger can cancel a ride");
        }

        // Cannot cancel if ride is already ACTIVE or FINISHED
        if (ride.getStatus() == RideStatus.ACTIVE || ride.getStatus() == RideStatus.FINISHED) {
            throw new BadRequestException("Cannot cancel a ride that is already " + ride.getStatus());
        }

        if (ride.getStatus() != RideStatus.PENDING && ride.getStatus() != RideStatus.ACCEPTED) {
            throw new BadRequestException("Ride cannot be canceled. Current status: " + ride.getStatus());
        }

        // Cannot cancel if ride is in less than 10 minutes
        if (ride.getScheduledFor() != null) {
            Instant now = Instant.now();
            Instant tenMinutesBefore = ride.getScheduledFor().minusSeconds(10 * 60);
            if (now.isAfter(tenMinutesBefore)) {
                throw new BadRequestException("Cannot cancel ride less than 10 minutes before scheduled start");
            }
        }

        cancelRide(ride, CancelReasonType.OTHER, "Canceled by passenger", passenger);
    }

    private void cancelRide(Ride ride, CancelReasonType reasonType, String reason, User canceledBy) {
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelReasonType(reasonType);
        ride.setCancelReason(reason);
        ride.setCanceledByUser(canceledBy);
        setDriverBusy(ride.getDriver(), false);
        rideRepository.save(ride);
        createCancellationNotifications(ride);
    }
    
    public Ride stopRide(Long rideId, Long driverId, RideStopRequest request) {
        Ride ride = getById(rideId);

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new BadRequestException("Driver is not assigned to this ride");
        }

        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new BadRequestException("Ride must be ACTIVE to be stopped. Current status: " + ride.getStatus());
        }

        // Create or find stop location
        Location stopLocation = locationRepository.findByAddressAndLatAndLng(
                request.stopAddress(), request.stopLat(), request.stopLng())
                .orElse(null);

        if (stopLocation == null) {
            stopLocation = new Location();
            stopLocation.setAddress(request.stopAddress());
            stopLocation.setLat(request.stopLat());
            stopLocation.setLng(request.stopLng());
            stopLocation = locationRepository.save(stopLocation);
        }

        RideWaypoint originalDestination = rideWaypointRepository
            .findFirstByRideOrderByWaypointOrderDesc(ride)
            .orElse(null);

        if (originalDestination != null) {
            // Calculate distance from stop to original destination using MapService
            EstimateRequest estimateRequest = new EstimateRequest(
                new LocationDTO(stopLocation.getLat().doubleValue(), stopLocation.getLng().doubleValue(), stopLocation.getAddress()),
                new LocationDTO(originalDestination.getLocation().getLat().doubleValue(), originalDestination.getLocation().getLng().doubleValue(), originalDestination.getLocation().getAddress()),
                List.of(),
                null
            );
            try {
                EstimateResponse estimate = mapService.estimateRide(estimateRequest);
                BigDecimal remainingDistance = BigDecimal.valueOf(estimate.distanceInKm());
                BigDecimal remainingCost = remainingDistance.multiply(ride.getPricingPricePerKm());

                // Subtract remaining cost from original total cost
                BigDecimal newCost = ride.getTotalCost().subtract(remainingCost);
                ride.setTotalCost(newCost.max(BigDecimal.ZERO)); // Ensure non-negative

                // Update total distance (subtract remaining distance)
                BigDecimal newDistance = ride.getTotalDistanceKm().subtract(remainingDistance);
                ride.setTotalDistanceKm(newDistance.max(BigDecimal.ZERO));
            } catch (Exception ex) {
                // If Mapbox (via MapService) is unavailable or fails, skip recalculation
                // and keep existing totalCost and totalDistanceKm to allow ride to be stopped.
            }
        }

        // Remove all waypoints after the start (keep start, remove destinations)
        rideWaypointRepository.deleteByRideAndWaypointOrderGreaterThan(ride, 0);

        // Update ride
        ride.setStoppedAt(Instant.now());
        ride.setStopLocation(stopLocation);
        ride.setEndTime(Instant.now());
        ride.setPaidAt(Instant.now());
        ride.setStatus(RideStatus.FINISHED);

        // Vehicle availability removed - no longer tracking
        setDriverBusy(ride.getDriver(), false);
        
        ride = rideRepository.save(ride);
        eventPublisher.publishEvent(new RideFinishedEvent(ride.getId()));
        return ride;
    }
    
    private void createCancellationNotifications(Ride ride) {
        // Notification for ordering passenger
        if (ride.getOrderingPassenger() != null) {
            Notification notification = new Notification();
            notification.setUser(ride.getOrderingPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_CANCELLED);
            notification.setMessage("Your ride has been cancelled");
            notificationRepository.save(notification);
        }
        
        // Notifications for linked passengers (only if registered)
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
        for (RidePassenger rp : ridePassengers) {
            User linkedUser = userRepository.findByEmail(rp.getPassengerEmail()).orElse(null);
            if (linkedUser != null) {
                Notification notification = new Notification();
                notification.setUser(linkedUser);
                notification.setRide(ride);
                notification.setType(NotificationType.RIDE_CANCELLED);
                notification.setMessage("Your ride has been cancelled");
                notificationRepository.save(notification);
            }
        }
    }
    
    public Page<Ride> getAdminRideHistory(Instant from, Instant to, List<RideStatus> statuses, 
                                         Boolean hasPanic, Pageable pageable) {
        final Instant fromFinal = from == null ? Instant.ofEpochMilli(0) : from;
        final Instant toFinal = to == null ? Instant.now() : to;
        
        return rideRepository.findAdminRideHistory(fromFinal, toFinal, statuses, hasPanic, pageable);
    }
    
    public Ride reorderRide(Long rideId, Long passengerId) {
        Ride originalRide = getById(rideId);
        
        // Check if passenger is the ordering passenger
        if (originalRide.getOrderingPassenger() == null || !originalRide.getOrderingPassenger().getId().equals(passengerId)) {
            throw new BadRequestException("Only the ordering passenger can reorder a ride");
        }
        
        // Get original waypoints
        List<RideWaypoint> originalWaypoints = getRideWaypoints(originalRide);
        
        // Create new ride request with same waypoints/options
        RideCreateRequest.WaypointRequest[] waypointRequests = originalWaypoints.stream()
            .map(wp -> new RideCreateRequest.WaypointRequest(
                wp.getLocation().getAddress(),
                wp.getLocation().getLat(),
                wp.getLocation().getLng(),
                wp.getWaypointOrder()
            ))
            .toArray(RideCreateRequest.WaypointRequest[]::new);
        
        // Get linked passenger emails from original ride
        List<RidePassenger> linkedPassengers = ridePassengerRepository.findByRide(originalRide);
        List<String> linkedEmails = linkedPassengers.stream()
            .map(RidePassenger::getPassengerEmail)
            .collect(Collectors.toList());
        
        // Convert vehicle type name string to enum
        com.ftn.drumigo.domain.enums.VehicleTypeName vehicleTypeName = 
            com.ftn.drumigo.domain.enums.VehicleTypeName.valueOf(originalRide.getPricingVehicleTypeName());
        
        // Preserve scheduled time if present
        Instant scheduledFor = originalRide.getScheduledFor();
        
        RideCreateRequest reorderRequest = new RideCreateRequest(
            java.util.Arrays.asList(waypointRequests),
            vehicleTypeName,
            originalRide.getBabyTransport(),
            originalRide.getPetTransport(),
            linkedEmails,
            scheduledFor // Preserve scheduled time
        );
        
        // Create new ride
        return create(passengerId, reorderRequest);
    }
    
    public List<com.ftn.drumigo.domain.Review> getRideReviews(Long rideId) {
        Ride ride = getById(rideId);
        return reviewRepository.findByRide(ride);
    }
    
    public List<com.ftn.drumigo.domain.PanicEvent> getRidePanicEvents(Long rideId) {
        Ride ride = getById(rideId);
        return panicEventRepository.findByRide(ride);
    }
    
    public List<RidePassenger> getRidePassengers(Ride ride) {
        return ridePassengerRepository.findByRide(ride);
    }
    
    public boolean hasPanic(Long rideId) {
        Ride ride = getById(rideId);
        List<PanicEvent> panicEvents = panicEventRepository.findByRide(ride);
        return !panicEvents.isEmpty();
    }
}

