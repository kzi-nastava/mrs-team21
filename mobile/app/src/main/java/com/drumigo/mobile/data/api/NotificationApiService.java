package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.NotificationResponse;
import com.drumigo.mobile.data.model.history.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Notifications API. Use for passenger and admin (GET user notifications).
 */
public interface NotificationApiService {

    @GET("users/{userId}/notifications")
    Call<PageResponse<NotificationResponse>> getUserNotifications(
        @Path("userId") long userId,
        @Query("page") int page,
        @Query("size") int size,
        @Query("sort") String sort
    );
}
