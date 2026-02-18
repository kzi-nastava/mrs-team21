package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.users.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Long> {
    List<FavoriteRoute> findByPassenger(Passenger passenger);

    Optional<FavoriteRoute> findByPassengerAndSourceRideId(Passenger passenger, Long sourceRideId);

    List<FavoriteRoute> findByPassengerAndSourceRideIdIn(Passenger passenger, Collection<Long> sourceRideIds);
}

