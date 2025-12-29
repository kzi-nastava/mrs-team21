package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.*;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.RideCreateRequest;
import com.ftn.drumigo.dto.RideInconsistencyCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
        
        if (ride.getVehicle() != null) {
            Vehicle vehicle = ride.getVehicle();
            vehicle.setAvailable(true);
            vehicleRepository.save(vehicle);
        }
        
        ride = rideRepository.save(ride);
        
        // Create notifications for ordering passenger and linked passengers
        createFinishNotifications(ride);
        
        return ride;
    }
    
    private void createFinishNotifications(Ride ride) {
        // Get all passengers for this ride
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
        
        // Create notification for each linked passenger
        for (RidePassenger rp : ridePassengers) {
            Notification notification = new Notification();
            notification.setUser(rp.getPassenger());
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_FINISHED);
            notification.setMessage("Your ride has finished");
            notificationRepository.save(notification);
        }
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
        
        // Get rides that started or ended within the last 24 hours
        List<Ride> recentRides = rideRepository.findByDriverAndStatus(driver, RideStatus.ACTIVE);
        recentRides.addAll(rideRepository.findByDriverAndStatus(driver, RideStatus.FINISHED));
        
        long totalSeconds = 0;
        for (Ride ride : recentRides) {
            // Check if ride overlaps with the 24-hour window
            // A ride contributes if it started within the window OR ended within the window
            boolean startedInWindow = ride.getStartTime() != null && 
                !ride.getStartTime().isBefore(twentyFourHoursAgo);
            boolean endedInWindow = ride.getEndTime() != null && 
                !ride.getEndTime().isBefore(twentyFourHoursAgo);
            
            if (startedInWindow || endedInWindow) {
                if (ride.getStatus() == RideStatus.FINISHED && ride.getStartTime() != null && ride.getEndTime() != null) {
                    // For finished rides, use actual duration
                    Instant rideStart = ride.getStartTime();
                    Instant rideEnd = ride.getEndTime();
                    // Only count the portion within the 24-hour window
                    Instant windowStart = rideStart.isBefore(twentyFourHoursAgo) ? twentyFourHoursAgo : rideStart;
                    Instant windowEnd = rideEnd.isAfter(now) ? now : rideEnd;
                    if (!windowStart.isAfter(windowEnd)) {
                        totalSeconds += Duration.between(windowStart, windowEnd).getSeconds();
                    }
                } else if (ride.getStatus() == RideStatus.ACTIVE && ride.getStartTime() != null) {
                    // For active rides, count from start time (or window start) to now
                    Instant rideStart = ride.getStartTime();
                    Instant windowStart = rideStart.isBefore(twentyFourHoursAgo) ? twentyFourHoursAgo : rideStart;
                    totalSeconds += Duration.between(windowStart, now).getSeconds();
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
}

