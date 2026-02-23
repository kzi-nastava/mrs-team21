package com.drumigo.mobile.data.model.report;

/**
 * One day of report data. Backend sends date as yyyy-MM-dd.
 */
public class ReportDayDto {
    public String date;
    public long rideCount;
    public long distanceKm;
    public double cost;
}
