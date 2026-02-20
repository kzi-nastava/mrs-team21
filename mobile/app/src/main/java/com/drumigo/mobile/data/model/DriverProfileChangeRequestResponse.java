package com.drumigo.mobile.data.model;

/**
 * Response from POST /api/drivers/{id}/profile-change-requests.
 */
public class DriverProfileChangeRequestResponse {
    public Long id;
    public Long driverId;
    public String driverName;
    public String driverSurname;
    public String requestedChangesJson;
    public String status;
    public String createdAt;
    public String reviewedAt;
    public Long reviewedByAdminId;
}
