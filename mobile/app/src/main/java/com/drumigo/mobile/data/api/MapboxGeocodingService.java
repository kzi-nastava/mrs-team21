package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.mapbox.MapboxGeocodingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MapboxGeocodingService {

    @GET("geocoding/v5/mapbox.places/{query}.json")
    Call<MapboxGeocodingResponse> geocodeAddress(
        @Path(value = "query", encoded = true) String query,
        @Query("limit") int limit,
        @Query("access_token") String accessToken
    );

    @GET("geocoding/v5/mapbox.places/{query}.json")
    Call<MapboxGeocodingResponse> searchAddressSuggestions(
        @Path(value = "query", encoded = true) String query,
        @Query("autocomplete") boolean autocomplete,
        @Query("limit") int limit,
        @Query("types") String types,
        @Query("access_token") String accessToken
    );

    @GET("geocoding/v5/mapbox.places/{longitude},{latitude}.json")
    Call<MapboxGeocodingResponse> reverseGeocodeAddress(
        @Path("longitude") String longitude,
        @Path("latitude") String latitude,
        @Query("limit") int limit,
        @Query("types") String types,
        @Query("access_token") String accessToken
    );
}
