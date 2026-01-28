package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.users.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Long> {
    List<FavoriteRoute> findByPassenger(Passenger passenger);
}

