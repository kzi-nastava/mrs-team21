package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.support.SupportConversationSummaryResponse;
import com.drumigo.mobile.data.model.support.SupportMessageCreateRequest;
import com.drumigo.mobile.data.model.support.SupportMessageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Support chat API. Base URL is API_BASE_URL (e.g. .../api).
 */
public interface SupportApiService {

    @GET("support/my-chat")
    Call<List<SupportMessageResponse>> getMyChat();

    @GET("support/conversations")
    Call<List<SupportConversationSummaryResponse>> getConversations();

    @GET("support/conversations/{userId}")
    Call<List<SupportMessageResponse>> getConversation(@Path("userId") long userId);

    @POST("support/messages")
    Call<SupportMessageResponse> sendMessage(@Body SupportMessageCreateRequest request);
}
