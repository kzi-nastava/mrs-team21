package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.FavoriteRouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRouteWaypointRepository extends JpaRepository<FavoriteRouteWaypoint, Long> {
    List<FavoriteRouteWaypoint> findByFavoriteRouteOrderByWaypointOrderAsc(FavoriteRoute favoriteRoute);
}

