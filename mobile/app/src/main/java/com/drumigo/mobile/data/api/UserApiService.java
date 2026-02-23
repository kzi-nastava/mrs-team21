package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.user.UserResponse;

import retrofit2.Call;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApiService {

    @PUT("users/{id}/block")
    Call<UserResponse> blockUser(@Path("id") long userId);

    @PUT("users/{id}/unblock")
    Call<UserResponse> unblockUser(@Path("id") long userId);
}
