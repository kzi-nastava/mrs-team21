package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.FavoriteRouteWaypoint;
import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Passenger;
import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.dto.FavoriteRouteCreateRequest;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.FavoriteRouteRepository;
import com.ftn.drumigo.repository.FavoriteRouteWaypointRepository;
import com.ftn.drumigo.repository.LocationRepository;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteRouteService {
    
    private final FavoriteRouteRepository favoriteRouteRepository;
    private final FavoriteRouteWaypointRepository waypointRepository;
    private final PassengerRepository passengerRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LocationRepository locationRepository;
    
    public FavoriteRoute create(Long passengerId, FavoriteRouteCreateRequest request) {
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        
        VehicleType vehicleType = vehicleTypeRepository.findById(request.vehicleTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with id: " + request.vehicleTypeId()));
        
        // Create favorite route
        FavoriteRoute favoriteRoute = new FavoriteRoute();
        favoriteRoute.setPassenger(passenger);
        favoriteRoute.setVehicleType(vehicleType);
        favoriteRoute.setBabyTransport(request.babyTransport() != null ? request.babyTransport() : false);
        favoriteRoute.setPetTransport(request.petTransport() != null ? request.petTransport() : false);
        
        favoriteRoute = favoriteRouteRepository.save(favoriteRoute);
        
        // Create waypoints
        for (FavoriteRouteCreateRequest.WaypointRequest wpRequest : request.waypoints()) {
            // Check if location already exists
            Location location = locationRepository.findByAddressAndLatAndLng(
                    wpRequest.address(), wpRequest.lat(), wpRequest.lng())
                    .orElse(null);
            
            if (location == null) {
                // Create new location only if it doesn't exist
                location = new Location();
                location.setAddress(wpRequest.address());
                location.setLat(wpRequest.lat());
                location.setLng(wpRequest.lng());
                location = locationRepository.save(location);
            }
            
            FavoriteRouteWaypoint waypoint = new FavoriteRouteWaypoint();
            waypoint.setFavoriteRoute(favoriteRoute);
            waypoint.setLocation(location);
            waypoint.setWaypointOrder(wpRequest.order());
            
            waypointRepository.save(waypoint);
        }
        
        return favoriteRoute;
    }
    
    public List<FavoriteRoute> getByPassenger(Long passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        
        return favoriteRouteRepository.findByPassenger(passenger);
    }
    
    public void delete(Long favoriteRouteId) {
        FavoriteRoute favoriteRoute = favoriteRouteRepository.findById(favoriteRouteId)
            .orElseThrow(() -> new ResourceNotFoundException("Favorite route not found with id: " + favoriteRouteId));
        
        // Delete waypoints first
        List<FavoriteRouteWaypoint> waypoints = waypointRepository.findByFavoriteRouteOrderByWaypointOrderAsc(favoriteRoute);
        waypointRepository.deleteAll(waypoints);
        
        // Delete favorite route
        favoriteRouteRepository.delete(favoriteRoute);
    }
    
    public List<FavoriteRouteWaypoint> getWaypoints(FavoriteRoute favoriteRoute) {
        return waypointRepository.findByFavoriteRouteOrderByWaypointOrderAsc(favoriteRoute);
    }
}

