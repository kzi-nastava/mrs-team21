package com.drumigo.mobile.data.model.ride;

public class LocationUpdate {
    public double lat;
    public double lng;
    public long timestamp;
    public int estimatedArrivalTimeSec;
    public float bearing;

    public LocationUpdate(double lat, double lng, long timestamp, int estimatedArrivalTimeSec, float bearing) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
        this.estimatedArrivalTimeSec = estimatedArrivalTimeSec;
        this.bearing = bearing;
    }
}
