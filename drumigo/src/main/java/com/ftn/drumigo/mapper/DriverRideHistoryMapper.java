package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.*;
import com.ftn.drumigo.dto.DriverRideHistoryItemResponse;
import com.ftn.drumigo.repository.PanicEventRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DriverRideHistoryMapper {
    
    private final RidePassengerRepository ridePassengerRepository;
    private final RideWaypointRepository rideWaypointRepository;
    private final PanicEventRepository panicEventRepository;
    
    public DriverRideHistoryItemResponse toResponse(Ride ride) {
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRideWithPassenger(ride);
        boolean panicOccurred = !panicEventRepository.findByRide(ride).isEmpty();
        
        RideWaypoint startWaypoint = waypoints.stream()
            .filter(wp -> wp.getWaypointOrder() == 0)
            .findFirst()
            .orElse(null);
        
        RideWaypoint endWaypoint = waypoints.stream()
            .filter(wp -> wp.getWaypointOrder() == waypoints.size() - 1)
            .findFirst()
            .orElse(null);
        
        DriverRideHistoryItemResponse.LocationInfo startLocation = null;
        DriverRideHistoryItemResponse.LocationInfo endLocation = null;
        
        if (startWaypoint != null && startWaypoint.getLocation() != null) {
            Location loc = startWaypoint.getLocation();
            startLocation = new DriverRideHistoryItemResponse.LocationInfo(
                loc.getAddress(),
                loc.getLat(),
                loc.getLng()
            );
        }
        
        if (endWaypoint != null && endWaypoint.getLocation() != null) {
            Location loc = endWaypoint.getLocation();
            endLocation = new DriverRideHistoryItemResponse.LocationInfo(
                loc.getAddress(),
                loc.getLat(),
                loc.getLng()
            );
        }
        
        List<DriverRideHistoryItemResponse.PassengerInfo> passengers = ridePassengers.stream()
            .map(rp -> new DriverRideHistoryItemResponse.PassengerInfo(
                rp.getPassenger().getId(),
                rp.getPassenger().getName(),
                rp.getPassenger().getSurname()
            ))
            .collect(Collectors.toList());
        
        return new DriverRideHistoryItemResponse(
            ride.getId(),
            ride.getStatus().name(),
            ride.getStartTime(),
            ride.getEndTime(),
            startLocation,
            endLocation,
            ride.getStatus().name().equals("CANCELLED"),
            ride.getCanceledByUser() != null ? ride.getCanceledByUser().getId() : null,
            ride.getCanceledByUser() != null ? ride.getCanceledByUser().getName() : null,
            ride.getCanceledByUser() != null ? ride.getCanceledByUser().getSurname() : null,
            ride.getTotalCost(),
            passengers,
            panicOccurred
        );
    }
}

