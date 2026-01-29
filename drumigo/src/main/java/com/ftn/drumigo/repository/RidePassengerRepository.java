package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RidePassengerRepository extends JpaRepository<RidePassenger, RidePassenger.RidePassengerId> {
    List<RidePassenger> findByRide(Ride ride);
    boolean existsByRideAndPassenger(Ride ride, User passenger);

    /**
     * Find ride passengers with eagerly fetched passenger data.
     * This avoids Hibernate proxy narrowing issues with joined inheritance.
     */
    @Query("SELECT rp FROM RidePassenger rp JOIN FETCH rp.passenger WHERE rp.ride = :ride")
    List<RidePassenger> findByRideWithPassenger(@Param("ride") Ride ride);
}

