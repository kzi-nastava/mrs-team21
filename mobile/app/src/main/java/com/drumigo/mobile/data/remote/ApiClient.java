package com.drumigo.mobile.data.remote;

import com.drumigo.mobile.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile Retrofit retrofit;

    private ApiClient() {
    }

    public static AuthApi getAuthApi() {
        return getRetrofit().create(AuthApi.class);
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
                            .client(buildClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }

    private static OkHttpClient buildClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://localhost/";
        }
        String normalized = baseUrl.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
