package com.drumigo.mobile.data.model.ride;

import java.util.List;

/**
 * Request body for POST /api/rides (create ride).
 * Matches backend RideCreateRequest.
 */
public class RideCreateRequest {

    public List<WaypointRequest> waypoints;
    public String vehicleType;
    public Boolean babyTransport;
    public Boolean petTransport;
    public List<String> linkedPassengerEmails;
    /** ISO-8601 instant or null for immediate ride */
    public String scheduledFor;

    public RideCreateRequest() {
    }

    public static class WaypointRequest {
        public String address;
        public Double lat;
        public Double lng;
        public Integer order;

        public WaypointRequest() {
        }

        public WaypointRequest(String address, Double lat, Double lng, Integer order) {
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.order = order;
        }
    }
}
