package com.drumigo.mobile.data.remote;

import com.drumigo.mobile.data.remote.dto.auth.request.LoginRequest;
import com.drumigo.mobile.data.remote.dto.auth.request.PassengerRegisterRequest;
import com.drumigo.mobile.data.remote.dto.auth.response.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("passengers")
    Call<Void> registerPassenger(@Body PassengerRegisterRequest request);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
