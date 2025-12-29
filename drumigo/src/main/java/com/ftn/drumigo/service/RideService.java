package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.*;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.RideInconsistencyCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}

