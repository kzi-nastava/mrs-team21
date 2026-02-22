package com.drumigo.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from POST /api/drivers or GET /api/drivers/{id}.
 */
public class DriverResponse {

    public final Long id;
    public final String name;
    public final String surname;
    public final String email;
    public final String address;
    public final String phone;
    @SerializedName("profilePictureUrl")
    public final String profilePictureUrl;
    public final Boolean blocked;
    @SerializedName("activeDriver")
    public final Boolean activeDriver;
    @SerializedName("isBusy")
    public final Boolean isBusy;
    @SerializedName("lastStateChangeAt")
    public final String lastStateChangeAt;
    @SerializedName("createdAt")
    public final String createdAt;
    @SerializedName("updatedAt")
    public final String updatedAt;

    public DriverResponse(
            Long id,
            String name,
            String surname,
            String email,
            String address,
            String phone,
            String profilePictureUrl,
            Boolean blocked,
            Boolean activeDriver,
            Boolean isBusy,
            String lastStateChangeAt,
            String createdAt,
            String updatedAt) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.profilePictureUrl = profilePictureUrl;
        this.blocked = blocked;
        this.activeDriver = activeDriver;
        this.isBusy = isBusy;
        this.lastStateChangeAt = lastStateChangeAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
