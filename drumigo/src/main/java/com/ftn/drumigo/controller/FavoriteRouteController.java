package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.FavoriteRouteWaypoint;
import com.ftn.drumigo.dto.FavoriteRouteCreateRequest;
import com.ftn.drumigo.dto.FavoriteRouteResponse;
import com.ftn.drumigo.mapper.FavoriteRouteMapper;
import com.ftn.drumigo.service.FavoriteRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FavoriteRouteController {
    
    private final FavoriteRouteService favoriteRouteService;
    private final FavoriteRouteMapper favoriteRouteMapper;
    
    @PostMapping("/passengers/{passengerId}/favorite-routes")
    public ResponseEntity<FavoriteRouteResponse> createFavoriteRoute(
            @PathVariable Long passengerId,
            @Valid @RequestBody FavoriteRouteCreateRequest request) {
        FavoriteRoute favoriteRoute = favoriteRouteService.create(passengerId, request);
        List<FavoriteRouteWaypoint> waypoints = favoriteRouteService.getWaypoints(favoriteRoute);
        return ResponseEntity.status(201).body(favoriteRouteMapper.toResponse(favoriteRoute, waypoints));
    }
    
    @GetMapping("/passengers/{passengerId}/favorite-routes")
    public ResponseEntity<List<FavoriteRouteResponse>> getFavoriteRoutes(@PathVariable Long passengerId) {
        List<FavoriteRoute> favoriteRoutes = favoriteRouteService.getByPassenger(passengerId);
        List<FavoriteRouteResponse> responses = favoriteRoutes.stream()
            .map(route -> {
                List<FavoriteRouteWaypoint> waypoints = favoriteRouteService.getWaypoints(route);
                return favoriteRouteMapper.toResponse(route, waypoints);
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/passengers/{passengerId}/favorite-routes/from-ride/{rideId}")
    public ResponseEntity<FavoriteRouteResponse> createFavoriteRouteFromRide(
            @PathVariable Long passengerId,
            @PathVariable Long rideId) {
        FavoriteRoute favoriteRoute = favoriteRouteService.createFromRide(passengerId, rideId);
        List<FavoriteRouteWaypoint> waypoints = favoriteRouteService.getWaypoints(favoriteRoute);
        return ResponseEntity.status(201).body(favoriteRouteMapper.toResponse(favoriteRoute, waypoints));
    }

    @DeleteMapping("/passengers/{passengerId}/favorite-routes/by-ride/{rideId}")
    public ResponseEntity<Void> deleteFavoriteRouteByRide(
            @PathVariable Long passengerId,
            @PathVariable Long rideId) {
        favoriteRouteService.deleteByPassengerAndSourceRideId(passengerId, rideId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/favorite-routes/{id}")
    public ResponseEntity<Void> deleteFavoriteRoute(@PathVariable Long id) {
        favoriteRouteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

