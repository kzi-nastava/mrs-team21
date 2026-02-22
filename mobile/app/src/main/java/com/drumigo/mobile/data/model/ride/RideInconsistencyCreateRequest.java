package com.drumigo.mobile.data.model.ride;

/**
 * Request body for POST /rides/{id}/inconsistencies.
 * Note must be 10–1000 characters.
 */
public class RideInconsistencyCreateRequest {
    public String note;

    public RideInconsistencyCreateRequest() {
    }

    public RideInconsistencyCreateRequest(String note) {
        this.note = note;
    }
}
