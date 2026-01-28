package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.*;
import com.ftn.drumigo.domain.enums.CancelReasonType;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.RideCreateRequest;
import com.ftn.drumigo.dto.RideInconsistencyCreateRequest;
import com.ftn.drumigo.dto.RideStopRequest;
import com.ftn.drumigo.dto.ride.request.RideCancelByDriverRequest;
import com.ftn.drumigo.event.RideFinishedEvent;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
    
    public List<Ride> getActiveRides() {
        return rideRepository.findByStatus(RideStatus.ACTIVE);
    }
    
    public Ride getById(Long id) {
        return rideRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }
    
    public List<RideWaypoint> getRideWaypoints(Ride ride) {
        return rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
    }
    
    public RideInconsistency createInconsistency(Long rideId, RideInconsistencyCreateRequest request) {
        Ride ride = getById(rideId);
        Passenger passenger = passengerRepository.findById(request.passengerId())
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + request.passengerId()));
        
        RideInconsistency inconsistency = new RideInconsistency();
        inconsistency.setRide(ride);
        inconsistency.setPassenger(passenger);
        inconsistency.setNote(request.note());
        
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
        
        if (ride.getVehicle() != null) {
            Vehicle vehicle = ride.getVehicle();
            vehicle.setAvailable(true);
            vehicleRepository.save(vehicle);
        }
        
        ride = rideRepository.save(ride);
        eventPublisher.publishEvent(new RideFinishedEvent(ride.getId()));
        
        return ride;
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
        
        // Calculate distance and ETA (stub implementation for KT1)
        BigDecimal totalDistance = calculateDistance(request.waypoints());
        ride.setTotalDistanceKm(totalDistance);
        
        // Calculate estimated cost
        BigDecimal totalCost = vehicleType.getStartPrice()
            .add(totalDistance.multiply(vehicleType.getPricePerKm()));
        ride.setTotalCost(totalCost);
        
        // Estimate duration (stub: assume 50 km/h average speed)
        if (totalDistance.compareTo(BigDecimal.ZERO) > 0) {
            int estimatedSeconds = totalDistance.divide(new BigDecimal("50"), 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("3600"))
                .intValue();
            ride.setEstimatedDurationSec(estimatedSeconds);
            
            if (request.scheduledFor() != null) {
                ride.setEstimatedArrivalAt(request.scheduledFor().plusSeconds(estimatedSeconds));
            } else {
                ride.setEstimatedArrivalAt(Instant.now().plusSeconds(estimatedSeconds));
            }
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
        
        // Assign driver (simple algorithm: first available driver with matching vehicle type)
        Driver assignedDriver = assignDriver(ride, vehicleType);
        if (assignedDriver != null) {
            ride.setDriver(assignedDriver);
            Vehicle vehicle = vehicleRepository.findByDriver(assignedDriver)
                .orElse(null);
            if (vehicle != null) {
                ride.setVehicle(vehicle);
                vehicle.setAvailable(false);
                vehicleRepository.save(vehicle);
            }
            ride.setStatus(RideStatus.ACCEPTED);
            
            // Create notifications
            createAcceptNotifications(ride);
        } else {
            ride.setStatus(RideStatus.REJECTED);
            // Create rejection notification for ordering passenger
            createRejectionNotification(ride);
        }
        
        ride = rideRepository.save(ride);
        
        // Add linked passengers with validation
        if (request.linkedPassengerEmails() != null && !request.linkedPassengerEmails().isEmpty()) {
            for (String email : request.linkedPassengerEmails()) {
                User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadRequestException("User with email " + email + " not found"));
                
                if (!(user instanceof Passenger passenger)) {
                    throw new BadRequestException("User with email " + email + " is not a passenger");
                }
                
                // Prevent linking the ordering passenger
                if (passenger.getId().equals(orderingPassengerId)) {
                    throw new BadRequestException("Cannot link the ordering passenger to their own ride");
                }
                
                RidePassenger ridePassenger = new RidePassenger();
                ridePassenger.setRide(ride);
                ridePassenger.setPassenger(passenger);
                ridePassengerRepository.save(ridePassenger);
                
                // Create notification for linked passenger
                Notification notification = new Notification();
                notification.setUser(passenger);
                notification.setRide(ride);
                notification.setType(NotificationType.LINKED_TO_RIDE);
                notification.setMessage("You have been linked to a ride");
                notificationRepository.save(notification);
            }
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
        
        // Update estimated arrival based on start time
        if (ride.getEstimatedDurationSec() != null) {
            ride.setEstimatedArrivalAt(Instant.now().plusSeconds(ride.getEstimatedDurationSec()));
        }
        
        ride = rideRepository.save(ride);
        
        // Create notifications
        createStartNotifications(ride);
        
        return ride;
    }
    
    private BigDecimal calculateDistance(List<RideCreateRequest.WaypointRequest> waypoints) {
        // Stub implementation: calculate simple distance between waypoints
        // In production, use a routing service
        if (waypoints.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalDistance = BigDecimal.ZERO;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            RideCreateRequest.WaypointRequest wp1 = waypoints.get(i);
            RideCreateRequest.WaypointRequest wp2 = waypoints.get(i + 1);
            
            // Haversine formula (simplified for KT1)
            double lat1 = wp1.lat().doubleValue();
            double lon1 = wp1.lng().doubleValue();
            double lat2 = wp2.lat().doubleValue();
            double lon2 = wp2.lng().doubleValue();
            
            double distance = haversineDistance(lat1, lon1, lat2, lon2);
            totalDistance = totalDistance.add(BigDecimal.valueOf(distance));
        }
        
        return totalDistance;
    }
    
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private Driver assignDriver(Ride ride, VehicleType vehicleType) {
        // Simple algorithm: find first available driver with matching vehicle type
        List<Driver> activeDrivers = driverRepository.findByActiveDriverTrue();
        
        for (Driver driver : activeDrivers) {
            // Check if driver has worked less than 8 hours in last 24 hours
            if (hasExceededWorkingHours(driver)) {
                continue;
            }
            
            // Check if driver has a vehicle of the requested type
            Vehicle vehicle = vehicleRepository.findByDriver(driver).orElse(null);
            if (vehicle != null && vehicle.getVehicleType().getId().equals(vehicleType.getId())) {
                // Check vehicle availability and requirements
                if (vehicle.getAvailable()) {
                    if (ride.getBabyTransport() && !vehicle.getBabyFriendly()) {
                        continue;
                    }
                    if (ride.getPetTransport() && !vehicle.getPetFriendly()) {
                        continue;
                    }
                    return driver;
                }
            }
        }
        
        return null;
    }
    
    private boolean hasExceededWorkingHours(Driver driver) {
        Instant now = Instant.now();
        Instant twentyFourHoursAgo = now.minus(Duration.ofHours(24));
        
        // Get all rides that started or ended within the last 24 hours (including CANCELLED)
        List<Ride> allRecentRides = rideRepository.findAll().stream()
            .filter(r -> r.getDriver() != null && r.getDriver().getId().equals(driver.getId()))
            .filter(r -> {
                boolean startedInWindow = r.getStartTime() != null && 
                    !r.getStartTime().isBefore(twentyFourHoursAgo);
                boolean endedInWindow = r.getEndTime() != null && 
                    !r.getEndTime().isBefore(twentyFourHoursAgo);
                return startedInWindow || endedInWindow;
            })
            .collect(Collectors.toList());
        
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
                // For cancelled rides that had started, count the time until cancellation
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
        
        // Notifications for linked passengers
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
        for (RidePassenger rp : ridePassengers) {
            Notification notification = new Notification();
            notification.setUser(rp.getPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_STARTED);
            notification.setMessage("Your ride has started");
            notificationRepository.save(notification);
        }
    }
    
    private void createRejectionNotification(Ride ride) {
        // Notification for ordering passenger when ride is rejected
        if (ride.getOrderingPassenger() != null) {
            Notification notification = new Notification();
            notification.setUser(ride.getOrderingPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_REJECTED);
            notification.setMessage("Your ride request has been rejected. No available driver found.");
            notificationRepository.save(notification);
        }
    }
    
    public void cancelByDriver(Long rideId, String email, RideCancelByDriverRequest request) {
        Ride ride = getById(rideId);
        Object driverObj = driverRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));
        if (!(driverObj instanceof Driver)) {
            throw new ResourceNotFoundException("Driver not found with email: " + email);
        }
        Driver driver = (Driver) driverObj;

        // Cannot cancel if driver is not assigned to this ride
        if (ride.getDriver() == null || !ride.getDriver().getEmail().equals(email)) {
            throw new BadRequestException("Driver is not assigned to this ride");
        }
        
        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.ACTIVE) {
            throw new BadRequestException("Ride cannot be cancelled. Current status: " + ride.getStatus());
        }
        
        cancelRide(ride, request.cancelReasonType(), request.reason(), driver);
    }
    
    public void cancelByPassenger(Long rideId, String email) {
        Ride ride = getById(rideId);
        Object passengerObject = passengerRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with email: " + email));
        if (!(passengerObject instanceof Passenger)) {
            throw new BadRequestException("User with email " + email + " is not a passenger");
        }
        Passenger passenger = (Passenger) passengerObject;
        // Check if passenger is the ordering passenger
        if (ride.getOrderingPassenger() == null || !ride.getOrderingPassenger().getEmail().equals(email)) {
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

        if (ride.getVehicle() != null) {
            Vehicle vehicle = ride.getVehicle();
            vehicle.setAvailable(true);
            vehicleRepository.save(vehicle);
        }

        rideRepository.save(ride);

        // TODO: create cancellation notifications
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
        
        // Basic validation: check if stop location is reasonable (not too far from route)
        // For KT1, we'll do a simple check: stop should be within reasonable distance from any waypoint
        List<RideWaypoint> currentWaypoints = getRideWaypoints(ride);
        boolean isReasonable = false;
        if (!currentWaypoints.isEmpty()) {
            for (RideWaypoint wp : currentWaypoints) {
                BigDecimal distance = calculateDistanceBetweenLocations(wp.getLocation(), stopLocation);
                // Allow stops within 50km of any waypoint (reasonable for ride-hailing)
                if (distance.compareTo(new BigDecimal("50")) <= 0) {
                    isReasonable = true;
                    break;
                }
            }
        }
        if (!isReasonable && !currentWaypoints.isEmpty()) {
            throw new BadRequestException("Stop location is too far from the ride route");
        }
        
        // Remove all waypoints after the start (we'll keep start and add stop as destination)
        // Remove waypoints with order > 0 (keep start at order 0, remove all destinations)
        for (RideWaypoint wp : currentWaypoints) {
            if (wp.getWaypointOrder() > 0) {
                rideWaypointRepository.delete(wp);
            }
        }
        
        // Update ride
        ride.setStoppedAt(Instant.now());
        ride.setStopLocation(stopLocation);
        
        // Recompute distance and price from start to stop location
        if (!currentWaypoints.isEmpty()) {
            Location startLocation = currentWaypoints.get(0).getLocation();
            BigDecimal newDistance = calculateDistanceBetweenLocations(startLocation, stopLocation);
            
            // Update total distance (partial)
            ride.setTotalDistanceKm(newDistance);
            
            // Recompute price
            BigDecimal newCost = ride.getPricingStartPrice()
                .add(newDistance.multiply(ride.getPricingPricePerKm()));
            ride.setTotalCost(newCost);
        }
        
        // Add stop as new destination waypoint (order 1, since start is order 0)
        RideWaypoint stopWaypoint = new RideWaypoint();
        stopWaypoint.setRide(ride);
        stopWaypoint.setLocation(stopLocation);
        stopWaypoint.setWaypointOrder(1);
        rideWaypointRepository.save(stopWaypoint);
        
        // Ride remains ACTIVE after stopping (spec 2.6.5 doesn't specify status change)
        ride = rideRepository.save(ride);
        
        return ride;
    }
    
    private BigDecimal calculateDistanceBetweenLocations(Location loc1, Location loc2) {
        double lat1 = loc1.getLat().doubleValue();
        double lon1 = loc1.getLng().doubleValue();
        double lat2 = loc2.getLat().doubleValue();
        double lon2 = loc2.getLng().doubleValue();
        
        double distance = haversineDistance(lat1, lon1, lat2, lon2);
        return BigDecimal.valueOf(distance).setScale(2, java.math.RoundingMode.HALF_UP);
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
        
        // Notifications for linked passengers
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
        for (RidePassenger rp : ridePassengers) {
            Notification notification = new Notification();
            notification.setUser(rp.getPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_CANCELLED);
            notification.setMessage("Your ride has been cancelled");
            notificationRepository.save(notification);
        }
    }
    
    public Page<Ride> getPassengerRideHistory(Long passengerId, Instant from, Instant to, 
                                              List<RideStatus> statuses, Boolean hasPanic, Pageable pageable) {
        final Instant fromFinal = from == null ? Instant.ofEpochMilli(0) : from;
        final Instant toFinal = to == null ? Instant.now() : to;
        
        // Get rides where passenger is ordering passenger (use inclusive boundaries)
        List<Ride> allRides = rideRepository.findAll().stream()
            .filter(r -> r.getOrderingPassenger() != null && 
                        r.getOrderingPassenger().getId().equals(passengerId) &&
                        !r.getRequestedAt().isBefore(fromFinal) && 
                        !r.getRequestedAt().isAfter(toFinal))
            .collect(Collectors.toList());
        
        // Also get rides where passenger is linked (use Set to avoid duplicates)
        Set<Long> rideIds = allRides.stream().map(Ride::getId).collect(Collectors.toSet());
        List<RidePassenger> linkedRides = ridePassengerRepository.findAll().stream()
            .filter(rp -> rp.getPassenger().getId().equals(passengerId))
            .collect(Collectors.toList());
        for (RidePassenger rp : linkedRides) {
            Ride ride = rp.getRide();
            if (!ride.getRequestedAt().isBefore(fromFinal) && !ride.getRequestedAt().isAfter(toFinal)) {
                if (!rideIds.contains(ride.getId())) {
                    allRides.add(ride);
                    rideIds.add(ride.getId());
                }
            }
        }
        
        // Filter by status if provided
        List<Ride> filteredByStatus;
        if (statuses != null && !statuses.isEmpty()) {
            filteredByStatus = allRides.stream()
                .filter(r -> statuses.contains(r.getStatus()))
                .collect(Collectors.toList());
        } else {
            filteredByStatus = allRides;
        }
        
        // Filter by panic if provided (optimize with repository query)
        List<Ride> finalRides;
        if (hasPanic != null && !filteredByStatus.isEmpty()) {
            List<PanicEvent> panicEvents = panicEventRepository.findByRideIn(filteredByStatus);
            Set<Long> ridesWithPanic = panicEvents.stream()
                .map(pe -> pe.getRide().getId())
                .collect(Collectors.toSet());
            
            if (hasPanic) {
                finalRides = filteredByStatus.stream()
                    .filter(r -> ridesWithPanic.contains(r.getId()))
                    .collect(Collectors.toList());
            } else {
                finalRides = filteredByStatus.stream()
                    .filter(r -> !ridesWithPanic.contains(r.getId()))
                    .collect(Collectors.toList());
            }
        } else {
            finalRides = filteredByStatus;
        }
        
        // Sort using pageable.sort instead of hardcoding
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            String property = order.getProperty();
            boolean ascending = order.getDirection().isAscending();
            
            finalRides.sort((r1, r2) -> {
                @SuppressWarnings("rawtypes")
                Comparable val1 = getSortValue(r1, property);
                @SuppressWarnings("rawtypes")
                Comparable val2 = getSortValue(r2, property);
                @SuppressWarnings({"rawtypes", "unchecked"})
                int result = val1.compareTo(val2);
                return ascending ? result : -result;
            });
        } else {
            // Default sort by requestedAt descending
            finalRides.sort((r1, r2) -> r2.getRequestedAt().compareTo(r1.getRequestedAt()));
        }
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), finalRides.size());
        List<Ride> pagedRides = finalRides.subList(start, end);
        
        return new PageImpl<>(
            pagedRides, pageable, finalRides.size());
    }
    
    @SuppressWarnings("rawtypes")
    private Comparable getSortValue(Ride ride, String property) {
        return switch (property) {
            case "requestedAt" -> ride.getRequestedAt();
            case "startTime" -> ride.getStartTime() != null ? ride.getStartTime() : Instant.ofEpochMilli(0);
            case "endTime" -> ride.getEndTime() != null ? ride.getEndTime() : Instant.ofEpochMilli(0);
            case "totalCost" -> ride.getTotalCost() != null ? ride.getTotalCost() : BigDecimal.ZERO;
            default -> ride.getRequestedAt();
        };
    }
    
    public Page<Ride> getAdminRideHistory(Instant from, Instant to, List<RideStatus> statuses, 
                                         Boolean hasPanic, Pageable pageable) {
        final Instant fromFinal = from == null ? Instant.ofEpochMilli(0) : from;
        final Instant toFinal = to == null ? Instant.now() : to;
        
        // Use inclusive boundaries
        List<Ride> allRides = rideRepository.findAll().stream()
            .filter(r -> !r.getRequestedAt().isBefore(fromFinal) && !r.getRequestedAt().isAfter(toFinal))
            .collect(Collectors.toList());
        
        // Filter by status if provided
        List<Ride> filteredByStatus;
        if (statuses != null && !statuses.isEmpty()) {
            filteredByStatus = allRides.stream()
                .filter(r -> statuses.contains(r.getStatus()))
                .collect(Collectors.toList());
        } else {
            filteredByStatus = allRides;
        }
        
        // Filter by panic if provided (optimize with repository query)
        List<Ride> finalRides;
        if (hasPanic != null && !filteredByStatus.isEmpty()) {
            List<PanicEvent> panicEvents = panicEventRepository.findByRideIn(filteredByStatus);
            Set<Long> ridesWithPanic = panicEvents.stream()
                .map(pe -> pe.getRide().getId())
                .collect(Collectors.toSet());
            
            if (hasPanic) {
                finalRides = filteredByStatus.stream()
                    .filter(r -> ridesWithPanic.contains(r.getId()))
                    .collect(Collectors.toList());
            } else {
                finalRides = filteredByStatus.stream()
                    .filter(r -> !ridesWithPanic.contains(r.getId()))
                    .collect(Collectors.toList());
            }
        } else {
            finalRides = filteredByStatus;
        }
        
        // Sort using pageable.sort instead of hardcoding
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            String property = order.getProperty();
            boolean ascending = order.getDirection().isAscending();
            
            finalRides.sort((r1, r2) -> {
                @SuppressWarnings("rawtypes")
                Comparable val1 = getSortValue(r1, property);
                @SuppressWarnings("rawtypes")
                Comparable val2 = getSortValue(r2, property);
                @SuppressWarnings({"rawtypes", "unchecked"})
                int result = val1.compareTo(val2);
                return ascending ? result : -result;
            });
        } else {
            // Default sort by requestedAt descending
            finalRides.sort((r1, r2) -> r2.getRequestedAt().compareTo(r1.getRequestedAt()));
        }
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), finalRides.size());
        List<Ride> pagedRides = finalRides.subList(start, end);
        
        return new PageImpl<>(
            pagedRides, pageable, finalRides.size());
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
            .map(rp -> rp.getPassenger().getEmail())
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

