package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideInconsistency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideInconsistencyRepository extends JpaRepository<RideInconsistency, Long> {
    List<RideInconsistency> findByRide(Ride ride);
}

