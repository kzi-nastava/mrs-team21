package com.drumigo.mobile.data.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.drumigo.mobile.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthSessionInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthSessionInterceptor(@NonNull Context context) {
        this.sessionManager = SessionManager.getInstance(context);
    }

    @Override
    @NonNull
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();

        String token = sessionManager.getToken();
        if (token != null && !token.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        Response response = chain.proceed(builder.build());
        if (response.code() == 401 || response.code() == 403) {
            sessionManager.markSessionExpired();
        }
        return response;
    }
}
