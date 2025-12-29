package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.dto.RideResponse;
import com.ftn.drumigo.dto.RideTrackingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RideMapper {
    
    public RideTrackingResponse toTrackingResponse(Ride ride, List<RideWaypoint> waypoints) {
        if (ride == null) {
            return null;
        }
        
        List<RideTrackingResponse.WaypointInfo> waypointInfos = waypoints.stream()
            .map(wp -> new RideTrackingResponse.WaypointInfo(
                wp.getLocation().getId(),
                wp.getLocation().getAddress(),
                wp.getLocation().getLat(),
                wp.getLocation().getLng(),
                wp.getWaypointOrder()
            ))
            .collect(Collectors.toList());
        
        return new RideTrackingResponse(
            ride.getId(),
            ride.getStatus().name(),
            ride.getRequestedAt(),
            ride.getStartTime(),
            ride.getDriver() != null ? ride.getDriver().getId() : null,
            ride.getDriver() != null ? ride.getDriver().getName() : null,
            ride.getDriver() != null ? ride.getDriver().getSurname() : null,
            ride.getVehicle() != null ? ride.getVehicle().getId() : null,
            ride.getVehicle() != null ? ride.getVehicle().getModel() : null,
            ride.getVehicle() != null ? ride.getVehicle().getLicensePlate() : null,
            ride.getVehicle() != null ? ride.getVehicle().getCurrentLat() : null,
            ride.getVehicle() != null ? ride.getVehicle().getCurrentLng() : null,
            waypointInfos,
            ride.getEstimatedArrivalAt(),
            ride.getEstimatedDurationSec(),
            ride.getTotalDistanceKm(),
            ride.getBabyTransport(),
            ride.getPetTransport()
        );
    }
    
    public RideResponse toResponse(Ride ride, List<RideWaypoint> waypoints) {
        if (ride == null) {
            return null;
        }
        
        List<RideResponse.WaypointInfo> waypointInfos = waypoints.stream()
            .map(wp -> new RideResponse.WaypointInfo(
                wp.getLocation().getId(),
                wp.getLocation().getAddress(),
                wp.getLocation().getLat(),
                wp.getLocation().getLng(),
                wp.getWaypointOrder()
            ))
            .collect(Collectors.toList());
        
        return new RideResponse(
            ride.getId(),
            ride.getStatus().name(),
            ride.getRequestedAt(),
            ride.getScheduledFor(),
            ride.getStartTime(),
            ride.getEndTime(),
            ride.getDriver() != null ? ride.getDriver().getId() : null,
            ride.getDriver() != null ? ride.getDriver().getName() : null,
            ride.getDriver() != null ? ride.getDriver().getSurname() : null,
            ride.getVehicle() != null ? ride.getVehicle().getId() : null,
            ride.getTotalCost(),
            ride.getTotalDistanceKm(),
            ride.getEstimatedDurationSec(),
            ride.getEstimatedArrivalAt(),
            ride.getBabyTransport(),
            ride.getPetTransport(),
            waypointInfos
        );
    }
}

