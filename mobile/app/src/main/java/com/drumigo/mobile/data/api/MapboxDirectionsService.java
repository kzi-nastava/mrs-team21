package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.mapbox.MapboxDirectionsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MapboxDirectionsService {

    @GET("directions/v5/mapbox/{profile}/{coordinates}")
    Call<MapboxDirectionsResponse> getDirections(
        @Path("profile") String profile,
        @Path(value = "coordinates", encoded = true) String coordinates,
        @Query("geometries") String geometries,
        @Query("overview") String overview,
        @Query("access_token") String accessToken
    );
}
