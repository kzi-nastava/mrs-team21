package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideWaypointRepository extends JpaRepository<RideWaypoint, Long> {
    List<RideWaypoint> findByRideOrderByWaypointOrderAsc(Ride ride);
}

