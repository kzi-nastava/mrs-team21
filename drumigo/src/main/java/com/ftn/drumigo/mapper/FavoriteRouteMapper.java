package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.FavoriteRouteWaypoint;
import com.ftn.drumigo.dto.FavoriteRouteResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FavoriteRouteMapper {
    
    public FavoriteRouteResponse toResponse(FavoriteRoute favoriteRoute, List<FavoriteRouteWaypoint> waypoints) {
        if (favoriteRoute == null) {
            return null;
        }
        
        List<FavoriteRouteResponse.WaypointResponse> waypointResponses = waypoints.stream()
            .map(wp -> new FavoriteRouteResponse.WaypointResponse(
                wp.getId(),
                wp.getLocation().getId(),
                wp.getLocation().getAddress(),
                wp.getLocation().getLat(),
                wp.getLocation().getLng(),
                wp.getWaypointOrder()
            ))
            .collect(Collectors.toList());
        
        return new FavoriteRouteResponse(
            favoriteRoute.getId(),
            favoriteRoute.getPassenger().getId(),
            favoriteRoute.getVehicleType().getId(),
            favoriteRoute.getVehicleType().getName().name(),
            favoriteRoute.getBabyTransport(),
            favoriteRoute.getPetTransport(),
            waypointResponses,
            favoriteRoute.getCreatedAt()
        );
    }
}

