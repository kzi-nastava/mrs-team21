package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RidePassengerRepository extends JpaRepository<RidePassenger, RidePassenger.RidePassengerId> {
    List<RidePassenger> findByRide(Ride ride);

    boolean existsByRideAndPassengerEmail(Ride ride, String passengerEmail);

    List<RidePassenger> findByPassengerEmail(String passengerEmail);
}

