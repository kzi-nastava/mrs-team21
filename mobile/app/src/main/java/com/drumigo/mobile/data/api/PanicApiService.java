package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.panic.PanicEventResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Panic events API for admin. GET /api/panic-events (admin only).
 */
public interface PanicApiService {

    @GET("panic-events")
    Call<PageResponse<PanicEventResponse>> getPanicEvents(
        @Query("page") int page,
        @Query("size") int size,
        @Query("sort") String sort
    );
}
