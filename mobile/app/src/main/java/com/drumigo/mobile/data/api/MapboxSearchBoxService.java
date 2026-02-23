package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.mapbox.MapboxSearchBoxSuggestResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Mapbox Search Box API for address autocomplete (same as frontend).
 * https://docs.mapbox.com/api/search/search-box/#suggest
 */
public interface MapboxSearchBoxService {

    @GET("search/searchbox/v1/suggest")
    Call<MapboxSearchBoxSuggestResponse> suggest(
        @Query("q") String query,
        @Query("access_token") String accessToken,
        @Query("session_token") String sessionToken,
        @Query("limit") int limit,
        @Query("proximity") String proximity
    );
}
