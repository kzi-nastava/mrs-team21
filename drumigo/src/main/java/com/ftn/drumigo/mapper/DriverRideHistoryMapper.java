package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.*;
import com.ftn.drumigo.dto.history.response.DriverRideHistoryItemResponse;
import com.ftn.drumigo.repository.PanicEventRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.domain.users.User;
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
    private final UserRepository userRepository;

    public DriverRideHistoryItemResponse toResponse(Ride ride) {
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        List<RidePassenger> ridePassengers = ridePassengerRepository.findByRide(ride);
        boolean panicOccurred = !panicEventRepository.findByRide(ride).isEmpty();
        
        RideWaypoint startWaypoint = waypoints.isEmpty() ? null : waypoints.get(0);
        RideWaypoint endWaypoint = waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1);
        
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
            .map(rp -> {
                // Try to find the user by email if they're registered
                User user = userRepository.findByEmail(rp.getPassengerEmail()).orElse(null);
                if (user != null) {
                    return new DriverRideHistoryItemResponse.PassengerInfo(
                        user.getId(),
                        user.getName(),
                        user.getSurname()
                    );
                } else {
                    // If user not registered, use email as name and null for other fields
                    return new DriverRideHistoryItemResponse.PassengerInfo(
                        null,
                        rp.getPassengerEmail(),
                        "(Unregistered)"
                    );
                }
            })
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

