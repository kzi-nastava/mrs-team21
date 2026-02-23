package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.report.ReportChartResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ReportApiService {

    /**
     * Get ride report for the current user (passenger or driver) in the given date range.
     * from and to are ISO-8601 instants (e.g. start of day for from, end of day for to).
     */
    @GET("reports/me")
    Call<ReportChartResponse> getMyReport(
        @Query("from") String from,
        @Query("to") String to
    );
}
