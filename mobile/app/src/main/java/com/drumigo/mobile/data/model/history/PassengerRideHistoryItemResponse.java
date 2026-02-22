package com.drumigo.mobile.data.model.history;

public class PassengerRideHistoryItemResponse {
    public Long id;
    public String status;
    public String requestedAt;
    public String scheduledFor;
    public String startTime;
    public String endTime;
    public String startAddress;
    public String destinationAddress;
    public Double totalCost;
    public Boolean canceled;
    public String canceledBy;
    public Boolean hasPanic;
    public Boolean isFavorite;
    public Long favoriteRouteId;
}
