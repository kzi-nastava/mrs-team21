package com.drumigo.mobile.data.model.ride;

public class RoutePoint {
    public double lat;
    public double lng;
    public int order;
    /** Optional address for display (e.g. in checkpoints). */
    public String address;

    public RoutePoint(double lat, double lng, int order) {
        this.lat = lat;
        this.lng = lng;
        this.order = order;
    }

    public RoutePoint(double lat, double lng, int order, String address) {
        this.lat = lat;
        this.lng = lng;
        this.order = order;
        this.address = address;
    }
}
