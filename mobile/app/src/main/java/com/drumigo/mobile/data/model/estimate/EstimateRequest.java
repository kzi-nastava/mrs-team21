package com.drumigo.mobile.data.model.estimate;

import java.util.List;

public class EstimateRequest {
    public LocationDto startLocation;
    public LocationDto destinationLocation;
    public List<LocationDto> waypoints;
    public String vehicleTypeName;

    public EstimateRequest(
        LocationDto startLocation,
        LocationDto destinationLocation,
        List<LocationDto> waypoints,
        String vehicleTypeName
    ) {
        this.startLocation = startLocation;
        this.destinationLocation = destinationLocation;
        this.waypoints = waypoints;
        this.vehicleTypeName = vehicleTypeName;
    }
}
