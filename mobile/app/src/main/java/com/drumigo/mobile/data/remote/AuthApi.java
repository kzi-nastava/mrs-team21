package com.drumigo.mobile.data.remote;

import com.drumigo.mobile.data.model.profile.ProfilePictureUploadResponse;
import com.drumigo.mobile.data.remote.dto.auth.request.LoginRequest;
import com.drumigo.mobile.data.remote.dto.auth.request.SetPasswordRequest;
import com.drumigo.mobile.data.remote.dto.auth.request.PassengerRegisterRequest;
import com.drumigo.mobile.data.remote.dto.auth.response.LoginResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AuthApi {

    @POST("passengers")
    Call<Void> registerPassenger(@Body PassengerRegisterRequest request);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /**
     * Upload profile picture for registration (no auth). Returns {"url": "..."}.
     * Use the returned URL as profilePictureUrl when registering driver or passenger.
     */
    @Multipart
    @POST("auth/profile-picture")
    Call<ProfilePictureUploadResponse> uploadProfilePictureForRegistration(@Part MultipartBody.Part file);

    /**
     * Set driver password (activation link). No auth. PUT /api/activation/{token}/set-password.
     */
    @PUT("activation/{token}/set-password")
    Call<Void> setDriverPassword(@Path("token") String token, @Body SetPasswordRequest request);
}
