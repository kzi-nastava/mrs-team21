package com.drumigo.mobile.data.model.profile;

/**
 * Request body for PUT /api/profile. Field names must match backend.
 */
public class ProfileUpdateRequest {
    public String name;
    public String surname;
    public String email;
    public String address;
    public String phone;
    public String profilePictureUrl;

    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String name, String surname, String email,
                                String address, String phone, String profilePictureUrl) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.profilePictureUrl = profilePictureUrl;
    }
}
