package com.drumigo.mobile.data.api;

import com.drumigo.mobile.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static VehicleApiService getVehicleApiService() {
        return getRetrofit().create(VehicleApiService.class);
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    private static String ensureTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://localhost:8080/api/";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
