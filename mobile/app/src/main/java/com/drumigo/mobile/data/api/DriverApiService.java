package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.history.DriverRideHistoryItemResponse;
import com.drumigo.mobile.data.model.history.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DriverApiService {

    @GET("drivers/{driverId}/rides/history")
    Call<PageResponse<DriverRideHistoryItemResponse>> getDriverRideHistory(
        @Path("driverId") long driverId,
        @Query("from") String from,
        @Query("to") String to,
        @Query("page") int page,
        @Query("size") int size,
        @Query("sort") String sort
    );
}
