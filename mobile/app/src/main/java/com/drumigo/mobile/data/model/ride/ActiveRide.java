package com.drumigo.mobile.data.model.ride;

import java.util.List;

public class ActiveRide {
    public Long id;
    public String status;
    public String driverName;
    public String driverSurname;
    public String vehicleModel;
    public String vehicleLicensePlate;
    public String startAddress;
    public String destinationAddress;
    public RoutePoint startLocation;
    public RoutePoint destinationLocation;
    public RoutePoint currentLocation;
    public Integer estimatedArrivalTimeSec;
    public List<RoutePoint> route;
}
