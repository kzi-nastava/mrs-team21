package com.drumigo.mobile.data.model;

public class DriverLocationUpdateRequest {
    public double lat;
    public double lng;

    public DriverLocationUpdateRequest(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
