package com.drumigo.mobile.data.model.profile;

import com.google.gson.annotations.SerializedName;

/**
 * Response from POST /api/profile/picture. Backend returns {"url": "..."}.
 */
public class ProfilePictureUploadResponse {
    @SerializedName("url")
    public String url;
}
