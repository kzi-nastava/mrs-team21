package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.dto.history.response.PassengerRideHistoryItemResponse;
import com.ftn.drumigo.dto.ride.response.RideResponse;
import com.ftn.drumigo.dto.RideTrackingResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RideMapper {

    /**
     * Build tracking response with optional ETA override (e.g. recalculated remaining duration for ACTIVE rides).
     * When overrideDurationSec and overrideEstimatedArrivalAt are non-null, they are used instead of ride's stored values.
     */
    public RideTrackingResponse toTrackingResponse(
            Ride ride,
            List<RideWaypoint> waypoints,
            Integer overrideDurationSec,
            Instant overrideEstimatedArrivalAt) {
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

        Integer durationSec = overrideDurationSec != null ? overrideDurationSec : ride.getEstimatedDurationSec();
        Instant arrivalAt = overrideEstimatedArrivalAt != null ? overrideEstimatedArrivalAt : ride.getEstimatedArrivalAt();

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
            arrivalAt,
            durationSec,
            ride.getTotalDistanceKm(),
            ride.getBabyTransport(),
            ride.getPetTransport()
        );
    }

    /** Delegates to {@link #toTrackingResponse(Ride, List, Integer, Instant)} with no override. */
    public RideTrackingResponse toTrackingResponse(Ride ride, List<RideWaypoint> waypoints) {
        return toTrackingResponse(ride, waypoints, null, null);
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
            ride.getPaidAt(),
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
    
    public PassengerRideHistoryItemResponse toPassengerHistoryResponse(Ride ride, List<RideWaypoint> waypoints, boolean hasPanic) {
        if (ride == null) {
            return null;
        }
        
        String startAddress = waypoints == null || waypoints.isEmpty() ? null : waypoints.get(0).getLocation().getAddress();
        String destinationAddress = waypoints == null || waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1).getLocation().getAddress();
        
        boolean canceled = ride.getStatus() == com.ftn.drumigo.domain.enums.RideStatus.CANCELLED;
        String canceledBy = canceled && ride.getCanceledByUser() != null 
            ? ride.getCanceledByUser().getName() + " " + ride.getCanceledByUser().getSurname()
            : null;
        
        return new PassengerRideHistoryItemResponse(
            ride.getId(),
            ride.getStatus(),
            ride.getRequestedAt(),
            ride.getScheduledFor(),
            ride.getStartTime(),
            ride.getEndTime(),
            startAddress,
            destinationAddress,
            ride.getTotalCost(),
            canceled,
            canceledBy,
            hasPanic,
            null,
            null
        );
    }

    public PassengerRideHistoryItemResponse toPassengerHistoryResponse(
            Ride ride, List<RideWaypoint> waypoints, boolean hasPanic, boolean isFavorite, Long favoriteRouteId) {
        PassengerRideHistoryItemResponse base = toPassengerHistoryResponse(ride, waypoints, hasPanic);
        if (base == null) {
            return null;
        }
        return new PassengerRideHistoryItemResponse(
            base.id(),
            base.status(),
            base.requestedAt(),
            base.scheduledFor(),
            base.startTime(),
            base.endTime(),
            base.startAddress(),
            base.destinationAddress(),
            base.totalCost(),
            base.canceled(),
            base.canceledBy(),
            base.hasPanic(),
            isFavorite,
            favoriteRouteId
        );
    }
}

