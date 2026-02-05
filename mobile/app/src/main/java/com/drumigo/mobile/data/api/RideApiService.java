package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RideApiService {

    @GET("rides/{id}")
    Call<RideTrackingResponse> getRideTracking(@Path("id") long rideId);

    @GET("rides/active")
    Call<List<RideResponse>> getActiveRides();
}
