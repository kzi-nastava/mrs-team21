package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.profile.PasswordUpdateRequest;
import com.drumigo.mobile.data.model.profile.ProfilePictureUploadResponse;
import com.drumigo.mobile.data.model.profile.ProfileResponse;
import com.drumigo.mobile.data.model.profile.ProfileUpdateRequest;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PUT;

/**
 * Profile API. All calls require JWT (use ApiClient.getProfileApiService()).
 */
public interface ProfileApiService {

    @GET("profile")
    Call<ProfileResponse> getProfile();

    @PUT("profile")
    Call<ProfileResponse> updateProfile(@Body ProfileUpdateRequest request);

    /**
     * Upload profile picture. Returns JSON with "url" key. Client should then call updateProfile with that URL.
     * Backend expects form field name "file" and content type image/jpeg, image/png, image/gif, or image/webp.
     */
    @Multipart
    @POST("profile/picture")
    Call<ProfilePictureUploadResponse> uploadProfilePicture(@Part MultipartBody.Part file);

    /**
     * Change password for the current user. Backend validates current password and new password (6+ chars, uppercase, number).
     */
    @PUT("profile/password")
    Call<Void> updatePassword(@Body PasswordUpdateRequest request);
}
