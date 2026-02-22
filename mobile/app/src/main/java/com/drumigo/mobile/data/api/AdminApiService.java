package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.data.model.user.UserResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AdminApiService {

    @GET("admin/users")
    Call<PageResponse<UserResponse>> getUsers(
        @Query("page") int page,
        @Query("size") int size,
        @Query("sort") String sort,
        @Query("role") String role
    );

    @GET("admin/rides/history")
    Call<PageResponse<RideResponse>> getAdminRideHistory(
        @Query("from") String from,
        @Query("to") String to,
        @Query("page") int page,
        @Query("size") int size,
        @Query("sort") String sort
    );
}
