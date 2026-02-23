package com.drumigo.mobile.data.model.ride;

import java.util.List;

public class RideResponse {
    public Long id;
    public String status;
    public String requestedAt;
    public String scheduledFor;
    public String startTime;
    public String endTime;
    public String paidAt;
    public Long driverId;
    public String driverName;
    public String driverSurname;
    public Long vehicleId;
    public Double totalCost;
    public Double totalDistanceKm;
    public Integer estimatedDurationSec;
    public String estimatedArrivalAt;
    public Boolean babyTransport;
    public Boolean petTransport;
    public List<WaypointInfo> waypoints;

    public static class WaypointInfo {
        public Long locationId;
        public String address;
        public Double lat;
        public Double lng;
        public Integer order;
    }
}
