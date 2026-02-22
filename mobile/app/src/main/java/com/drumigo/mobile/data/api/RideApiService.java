package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.estimate.EstimateRequest;
import com.drumigo.mobile.data.model.estimate.EstimateResponse;
import com.drumigo.mobile.data.model.ride.ActiveRideIdResponse;
import com.drumigo.mobile.data.model.ride.RideCancelByDriverRequest;
import com.drumigo.mobile.data.model.ride.RideCreateRequest;
import com.drumigo.mobile.data.model.ride.RideDetailsResponse;
import com.drumigo.mobile.data.model.ride.RideInconsistencyCreateRequest;
import com.drumigo.mobile.data.model.ride.RideInconsistencyResponse;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.data.model.ride.RideStopRequest;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RideApiService {

    @GET("rides/{id}")
    Call<RideTrackingResponse> getRideTracking(@Path("id") long rideId);

    @GET("rides/{id}/details")
    Call<RideDetailsResponse> getRideDetails(@Path("id") long rideId);

    @GET("rides/active")
    Call<List<RideResponse>> getActiveRides();

    @GET("rides/me/active")
    Call<ActiveRideIdResponse> getMyActiveRide();

    @POST("rides")
    Call<RideResponse> createRide(@Body RideCreateRequest request);

    @POST("rides/estimate")
    Call<EstimateResponse> estimateRide(@Body EstimateRequest request);

    @POST("rides/{id}/panic")
    Call<Void> createPanic(@Path("id") long rideId);

    @PUT("rides/{id}/cancel-by-driver")
    Call<Void> cancelRideByDriver(@Path("id") long rideId, @Body RideCancelByDriverRequest request);

    @PUT("rides/{id}/cancel-by-passenger")
    Call<Void> cancelRideByPassenger(@Path("id") long rideId);

    @PUT("rides/{id}/stop")
    Call<RideResponse> stopRide(@Path("id") long rideId, @Body RideStopRequest request);

    @PUT("rides/{id}/start")
    Call<RideResponse> startRide(@Path("id") long rideId);

    @PUT("rides/{id}/end")
    Call<RideResponse> endRide(@Path("id") long rideId);

    @POST("rides/{id}/inconsistencies")
    Call<RideInconsistencyResponse> reportInconsistency(
        @Path("id") long rideId,
        @Body RideInconsistencyCreateRequest request
    );

    @GET("rides/{id}/inconsistencies")
    Call<List<RideInconsistencyResponse>> getInconsistencies(@Path("id") long rideId);
}
