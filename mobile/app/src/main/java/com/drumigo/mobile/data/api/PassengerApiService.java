package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.favorite.FavoriteRouteResponse;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.history.PassengerRideHistoryItemResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
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

    @GET("passengers/{passengerId}/favorite-routes")
    Call<List<FavoriteRouteResponse>> getFavoriteRoutes(@Path("passengerId") long passengerId);

    @POST("passengers/{passengerId}/favorite-routes/from-ride/{rideId}")
    Call<FavoriteRouteResponse> createFavoriteRouteFromRide(
        @Path("passengerId") long passengerId,
        @Path("rideId") long rideId
    );

    @DELETE("passengers/{passengerId}/favorite-routes/by-ride/{rideId}")
    Call<Void> deleteFavoriteRouteByRide(
        @Path("passengerId") long passengerId,
        @Path("rideId") long rideId
    );
}
