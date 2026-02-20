package com.drumigo.mobile.data.model.ride;

public class RideStopRequest {
    public String stopAddress;
    public Double stopLat;
    public Double stopLng;

    public RideStopRequest(String stopAddress, Double stopLat, Double stopLng) {
        this.stopAddress = stopAddress;
        this.stopLat = stopLat;
        this.stopLng = stopLng;
    }
}
