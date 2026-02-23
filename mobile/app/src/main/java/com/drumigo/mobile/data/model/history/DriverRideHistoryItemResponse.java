package com.drumigo.mobile.data.model.history;

import java.util.List;

public class DriverRideHistoryItemResponse {
    public Long id;
    public String status;
    public String startTime;
    public String endTime;
    public LocationInfo startLocation;
    public LocationInfo endLocation;
    public Boolean cancelled;
    public Long canceledByUserId;
    public String canceledByName;
    public String canceledBySurname;
    public Double totalCost;
    public List<PassengerInfo> passengers;
    public Boolean panicOccurred;
}
