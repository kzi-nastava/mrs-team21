package com.drumigo.mobile.data.model;

/**
 * Request body for POST /api/drivers/{id}/profile-change-requests.
 */
public class DriverProfileChangeRequestCreateRequest {
    public String requestedChangesJson;

    public DriverProfileChangeRequestCreateRequest() {
    }

    public DriverProfileChangeRequestCreateRequest(String requestedChangesJson) {
        this.requestedChangesJson = requestedChangesJson;
    }
}
