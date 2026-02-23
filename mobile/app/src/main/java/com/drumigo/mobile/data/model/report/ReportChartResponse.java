package com.drumigo.mobile.data.model.report;

import java.util.List;

/**
 * Response from GET /api/reports/me. from/to are ISO-8601 instants.
 */
public class ReportChartResponse {
    public String from;
    public String to;
    public List<ReportDayDto> dailyData;
    public long totalRides;
    public long totalDistanceKm;
    public double totalCost;
    public double avgRidesPerDay;
    public double avgDistanceKmPerDay;
    public double avgCostPerDay;
}
