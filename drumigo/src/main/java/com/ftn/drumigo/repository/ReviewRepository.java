package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRide(Ride ride);
    
    @Query("SELECT r FROM Review r WHERE r.ride.driver = :driver")
    List<Review> findByDriver(@Param("driver") Driver driver);
    
    Optional<Review> findByRideAndPassenger(Ride ride, Passenger passenger);
}

