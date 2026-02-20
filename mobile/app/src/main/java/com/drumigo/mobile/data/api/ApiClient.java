package com.drumigo.mobile.data.api;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.DrumigoApplication;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static VehicleApiService getVehicleApiService() {
        return getRetrofit().create(VehicleApiService.class);
    }

    public static RideApiService getRideApiService() {
        return getRetrofit().create(RideApiService.class);
    }

    public static DriverApiService getDriverApiService() {
        return getRetrofit().create(DriverApiService.class);
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
                .client(buildClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    private static OkHttpClient buildClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor);

        if (DrumigoApplication.getAppContext() != null) {
            builder.addInterceptor(new AuthSessionInterceptor(DrumigoApplication.getAppContext()));
        }
        return builder.build();
    }

    private static String ensureTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://localhost:8080/api/";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
