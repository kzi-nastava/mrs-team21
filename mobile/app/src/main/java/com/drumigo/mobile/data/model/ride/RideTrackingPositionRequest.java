package com.drumigo.mobile.data.model.ride;

/**
 * Request body for PUT /rides/{id}/tracking-position.
 * Sends the displayed (e.g. capped) position so backend stays in sync with the client.
 */
public class RideTrackingPositionRequest {
    public final double lat;
    public final double lng;

    public RideTrackingPositionRequest(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
