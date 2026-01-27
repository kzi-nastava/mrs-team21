package com.ftn.drumigo.dto.ride.request;

import com.ftn.drumigo.dto.map.LocationDTO;
import com.ftn.drumigo.domain.enums.VehicleTypeName;

import java.util.List;
import jakarta.validation.constraints.NotNull;

public record EstimateRequest(
   
    @NotNull(message = "Start location is required")
    LocationDTO startLocation,
    
    @NotNull(message = "Destination location is required")
    LocationDTO destinationLocation,

    List<LocationDTO> waypoints,

    VehicleTypeName vehicleTypeName
) {}

