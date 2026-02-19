package com.drumigo.mobile.data.model.estimate;

import java.util.List;

public class EstimateResponse {
    public String routePolyline;
    public List<List<Double>> routeCoordinates;
    public Double distanceInKm;
    public Integer durationInMinutes;
    public Double estimatedPrice;
}
