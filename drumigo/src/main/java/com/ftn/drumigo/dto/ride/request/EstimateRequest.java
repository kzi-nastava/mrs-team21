package com.ftn.drumigo.dto.ride.request;

import com.ftn.drumigo.dto.map.LocationDTO;
import com.ftn.drumigo.domain.enums.VehicleTypeName;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record EstimateRequest(
   
    @Valid
    @NotNull(message = "Start location is required")
    LocationDTO startLocation,
    
    @Valid
    @NotNull(message = "Destination location is required")
    LocationDTO destinationLocation,

    @Valid
    List<LocationDTO> waypoints,

    VehicleTypeName vehicleTypeName
) {}

