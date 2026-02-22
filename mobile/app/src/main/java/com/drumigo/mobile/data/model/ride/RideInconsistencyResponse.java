package com.drumigo.mobile.data.model.ride;

/**
 * Response from POST /rides/{id}/inconsistencies or GET /rides/{id}/inconsistencies.
 */
public class RideInconsistencyResponse {
    public Long id;
    public Long rideId;
    public Long passengerId;
    public String passengerName;
    public String passengerSurname;
    public String note;
    /** ISO-8601 date string from backend */
    public String createdAt;
}
