package com.drumigo.mobile.data.remote.dto.auth.request;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for PUT /api/activation/{token}/set-password (driver activation).
 */
public class SetPasswordRequest {
    @SerializedName("password")
    public final String password;

    public SetPasswordRequest(String password) {
        this.password = password;
    }
}
