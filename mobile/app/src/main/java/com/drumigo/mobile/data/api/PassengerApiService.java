package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.history.PassengerRideHistoryItemResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PassengerApiService {

    @GET("passengers/{passengerId}/rides/history")
    Call<PageResponse<PassengerRideHistoryItemResponse>> getPassengerRideHistory(
        @Path("passengerId") long passengerId,
        @Query("from") String from,
        @Query("to") String to,
        @Query("page") int page,
        @Query("size") int size,
        @Query("sort") String sort
    );
}
