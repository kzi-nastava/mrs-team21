package com.drumigo.mobile.data.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapboxApiClient {

    private static final String MAPBOX_BASE_URL = "https://api.mapbox.com/";
    private static Retrofit retrofit;

    private MapboxApiClient() {
    }

    public static MapboxDirectionsService getDirectionsService() {
        return getRetrofit().create(MapboxDirectionsService.class);
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(MAPBOX_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }
}
