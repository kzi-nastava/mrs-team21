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

import com.ftn.drumigo.dto.RideRatingStatusResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    
    /** Rating deadline: 3 days (72 hours) from ride end time */
    private static final long RATING_DEADLINE_HOURS = 72;
    
    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final RidePassengerRepository ridePassengerRepository;
    
    /**
     * Create a review for a ride using the passenger's email (from JWT principal).
     * This is the preferred method for authenticated requests.
     */
    public Review createReviewByEmail(Long rideId, String passengerEmail, ReviewCreateRequest request) {
        Passenger passenger = passengerRepository.findByEmail(passengerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with email: " + passengerEmail));
        return createReview(rideId, passenger.getId(), request);
    }
    
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
        
        // Check if rating deadline has passed (strict 72h check)
        if (ride.getEndTime() == null) {
            throw new BadRequestException("Ride has no end time");
        }
        
        Instant deadline = ride.getEndTime().plus(RATING_DEADLINE_HOURS, ChronoUnit.HOURS);
        if (Instant.now().isAfter(deadline)) {
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
    
    /**
     * Get the rating status for a ride using the passenger's email (from JWT principal).
     * This is the preferred method for authenticated requests.
     */
    @Transactional(readOnly = true)
    public RideRatingStatusResponse getRatingStatusByEmail(Long rideId, String passengerEmail) {
        Passenger passenger = passengerRepository.findByEmail(passengerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with email: " + passengerEmail));
        return getRatingStatus(rideId, passenger.getId());
    }
    
    /**
     * Get the rating status for a ride and passenger.
     * Returns whether the passenger can rate, if they already have a review,
     * days remaining to rate, and the rating deadline.
     * 
     * Security: Only returns status if passenger is part of the ride.
     */
    @Transactional(readOnly = true)
    public RideRatingStatusResponse getRatingStatus(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        
        // Security: verify passenger is part of this ride (prevent info leakage)
        if (!ridePassengerRepository.existsByRideAndPassenger(ride, passenger)) {
            throw new BadRequestException("Passenger is not part of this ride");
        }
        
        // Check if review already exists
        boolean hasReview = reviewRepository.findByRideAndPassenger(ride, passenger).isPresent();
        
        // Calculate deadline and days remaining using consistent 72h logic
        Instant ratingDeadline = null;
        int daysRemaining = 0;
        boolean canRate = false;
        
        if (ride.getEndTime() != null) {
            ratingDeadline = ride.getEndTime().plus(RATING_DEADLINE_HOURS, ChronoUnit.HOURS);
            Instant now = Instant.now();
            
            // Calculate days remaining (floor division for display)
            if (now.isBefore(ratingDeadline)) {
                long hoursRemaining = Duration.between(now, ratingDeadline).toHours();
                daysRemaining = (int) (hoursRemaining / 24);
            }
            
            // Can rate if: ride is finished, within deadline, is ordering passenger, and no existing review
            boolean isOrderingPassenger = ride.getOrderingPassenger() != null
                && Objects.equals(ride.getOrderingPassenger().getId(), passengerId);
            boolean withinDeadline = now.isBefore(ratingDeadline) || now.equals(ratingDeadline);
            boolean isFinished = ride.getStatus() == RideStatus.FINISHED;
            
            canRate = isFinished && withinDeadline && isOrderingPassenger && !hasReview;
        }
        
        return new RideRatingStatusResponse(canRate, hasReview, daysRemaining, ratingDeadline);
    }
}

