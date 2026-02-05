package com.drumigo.mobile.data.model.ride;

public class RoutePoint {
    public double lat;
    public double lng;
    public int order;

    public RoutePoint(double lat, double lng, int order) {
        this.lat = lat;
        this.lng = lng;
        this.order = order;
    }
}
