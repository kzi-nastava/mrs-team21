package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.estimate.EstimateRequest;
import com.drumigo.mobile.data.model.estimate.EstimateResponse;
import com.drumigo.mobile.data.model.ride.ActiveRideIdResponse;
import com.drumigo.mobile.data.model.ride.RideDetailsResponse;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.data.model.ride.RideStopRequest;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.PUT;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;

public interface RideApiService {

    @GET("rides/{id}")
    Call<RideTrackingResponse> getRideTracking(@Path("id") long rideId);

    @GET("rides/{id}/details")
    Call<RideDetailsResponse> getRideDetails(@Path("id") long rideId);

    @GET("rides/active")
    Call<List<RideResponse>> getActiveRides();

    @GET("rides/me/active")
    Call<ActiveRideIdResponse> getMyActiveRide();

    @POST("rides/estimate")
    Call<EstimateResponse> estimateRide(@Body EstimateRequest request);

    @POST("rides/{id}/panic")
    Call<Void> createPanic(@Path("id") long rideId);

    @PUT("rides/{id}/stop")
    Call<RideResponse> stopRide(@Path("id") long rideId, @Body RideStopRequest request);
}
