package com.drumigo.mobile.data.model.ride;

import java.util.List;

public class RideTrackingResponse {
    public Long id;
    public String status;
    public String requestedAt;
    public String startTime;
    public Long driverId;
    public String driverName;
    public String driverSurname;
    public Long vehicleId;
    public String vehicleModel;
    public String vehicleLicensePlate;
    public Double vehicleCurrentLat;
    public Double vehicleCurrentLng;
    public List<WaypointInfo> waypoints;
    public String estimatedArrivalAt;
    public Integer estimatedDurationSec;
    public Double totalDistanceKm;
    public Boolean babyTransport;
    public Boolean petTransport;

    public static class WaypointInfo {
        public Long locationId;
        public String address;
        public Double lat;
        public Double lng;
        public Integer order;
    }
}
