package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Driver;
import com.ftn.drumigo.domain.Passenger;
import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.ReviewCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.DriverRepository;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.ReviewRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final RidePassengerRepository ridePassengerRepository;
    
    public Review createReview(Long rideId, Long passengerId, ReviewCreateRequest request) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        
        // Validate that passenger is part of this ride
        if (!ridePassengerRepository.existsByRideAndPassenger(ride, passenger)) {
            throw new BadRequestException("Passenger is not part of this ride");
        }
        
        // Validate that passenger is the ordering passenger (only ordering passenger can review)
        if (ride.getOrderingPassenger() == null) {
            throw new BadRequestException("Ride has no ordering passenger set");
        }
        if (!ride.getOrderingPassenger().getId().equals(passengerId)) {
            throw new BadRequestException("Only the ordering passenger can create a review for this ride");
        }
        
        // Check if ride is finished
        if (ride.getStatus() != RideStatus.FINISHED) {
            throw new BadRequestException("Can only review finished rides");
        }
        
        // Check if 3 days have passed
        if (ride.getEndTime() == null) {
            throw new BadRequestException("Ride has no end time");
        }
        
        Duration duration = Duration.between(ride.getEndTime(), Instant.now());
        if (duration.toDays() > 3) {
            throw new BadRequestException("Review deadline has passed (3 days from ride end)");
        }
        
        // Check if review already exists
        if (reviewRepository.findByRideAndPassenger(ride, passenger).isPresent()) {
            throw new ConflictException("Review already exists for this ride and passenger");
        }
        
        Review review = new Review();
        review.setRide(ride);
        review.setPassenger(passenger);
        review.setRatingDriver(request.ratingDriver());
        review.setRatingVehicle(request.ratingVehicle());
        review.setComment(request.comment());
        
        return reviewRepository.save(review);
    }
    
    public List<Review> getRideReviews(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        return reviewRepository.findByRide(ride);
    }
    
    public List<Review> getDriverReviews(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        return reviewRepository.findByDriver(driver);
    }
}

