package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.FavoriteRoute;
import com.ftn.drumigo.domain.FavoriteRouteWaypoint;
import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.domain.enums.VehicleTypeName;
import com.ftn.drumigo.dto.FavoriteRouteCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.FavoriteRouteRepository;
import com.ftn.drumigo.repository.FavoriteRouteWaypointRepository;
import com.ftn.drumigo.repository.LocationRepository;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
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
    private final RideService rideService;
    private final RidePassengerRepository ridePassengerRepository;
    
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

    /**
     * Create a favorite route from a completed ride. The passenger must be the ordering passenger or a linked passenger.
     * Returns 409 if this ride is already a favorite for this passenger.
     */
    public FavoriteRoute createFromRide(Long passengerId, Long rideId) {
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        Ride ride = rideService.getById(rideId);

        boolean isOrdering = ride.getOrderingPassenger() != null
            && ride.getOrderingPassenger().getId().equals(passengerId);
        boolean isLinked = ridePassengerRepository.existsByRideAndPassengerEmail(ride, passenger.getEmail());
        if (!isOrdering && !isLinked) {
            throw new BadRequestException("You are not a passenger of this ride");
        }

        if (favoriteRouteRepository.findByPassengerAndSourceRideId(passenger, rideId).isPresent()) {
            throw new ConflictException("This ride is already in your favorites");
        }

        String typeName = ride.getPricingVehicleTypeName();
        if (typeName == null || typeName.isBlank()) {
            throw new BadRequestException("Ride has no vehicle type; cannot create favorite");
        }
        VehicleType vehicleType = vehicleTypeRepository.findByName(VehicleTypeName.valueOf(typeName))
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + typeName));

        List<RideWaypoint> rideWaypoints = rideService.getRideWaypoints(ride);
        if (rideWaypoints.size() < 2) {
            throw new BadRequestException("Ride must have at least 2 waypoints to save as favorite");
        }

        FavoriteRoute favoriteRoute = new FavoriteRoute();
        favoriteRoute.setPassenger(passenger);
        favoriteRoute.setVehicleType(vehicleType);
        favoriteRoute.setBabyTransport(ride.getBabyTransport() != null ? ride.getBabyTransport() : false);
        favoriteRoute.setPetTransport(ride.getPetTransport() != null ? ride.getPetTransport() : false);
        favoriteRoute.setSourceRideId(rideId);
        favoriteRoute = favoriteRouteRepository.save(favoriteRoute);

        for (RideWaypoint rw : rideWaypoints) {
            Location loc = rw.getLocation();
            Location location = locationRepository.findByAddressAndLatAndLng(
                    loc.getAddress(), loc.getLat(), loc.getLng())
                .orElseGet(() -> {
                    Location newLoc = new Location();
                    newLoc.setAddress(loc.getAddress());
                    newLoc.setLat(loc.getLat());
                    newLoc.setLng(loc.getLng());
                    return locationRepository.save(newLoc);
                });
            FavoriteRouteWaypoint waypoint = new FavoriteRouteWaypoint();
            waypoint.setFavoriteRoute(favoriteRoute);
            waypoint.setLocation(location);
            waypoint.setWaypointOrder(rw.getWaypointOrder());
            waypointRepository.save(waypoint);
        }

        return favoriteRoute;
    }

    /**
     * Delete the favorite route that was created from the given ride for this passenger. 404 if not found.
     */
    public void deleteByPassengerAndSourceRideId(Long passengerId, Long rideId) {
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        FavoriteRoute favoriteRoute = favoriteRouteRepository.findByPassengerAndSourceRideId(passenger, rideId)
            .orElseThrow(() -> new ResourceNotFoundException("No favorite route found for this ride"));
        List<FavoriteRouteWaypoint> waypoints = waypointRepository.findByFavoriteRouteOrderByWaypointOrderAsc(favoriteRoute);
        waypointRepository.deleteAll(waypoints);
        favoriteRouteRepository.delete(favoriteRoute);
    }

    public List<FavoriteRouteWaypoint> getWaypoints(FavoriteRoute favoriteRoute) {
        return waypointRepository.findByFavoriteRouteOrderByWaypointOrderAsc(favoriteRoute);
    }
}

