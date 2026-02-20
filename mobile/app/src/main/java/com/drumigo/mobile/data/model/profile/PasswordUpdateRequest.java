package com.drumigo.mobile.data.model.profile;

/**
 * Request body for PUT /api/profile/password (change password for current user).
 * Matches backend dto.PasswordUpdateRequest: currentPassword, newPassword.
 */
public class PasswordUpdateRequest {
    public final String currentPassword;
    public final String newPassword;

    public PasswordUpdateRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
