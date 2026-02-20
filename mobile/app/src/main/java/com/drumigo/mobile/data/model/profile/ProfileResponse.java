package com.drumigo.mobile.data.model.profile;

/**
 * Matches backend ProfileResponse. Field names must match JSON (name/surname, etc.).
 */
public class ProfileResponse {
    public Long id;
    public String name;
    public String surname;
    public String email;
    public String address;
    public String phone;
    public String profilePictureUrl;
    public Boolean blocked;
    /** PASSENGER, DRIVER, or ADMIN */
    public String role;
    public String createdAt;
    public String updatedAt;
    public Boolean activeDriver;
    public Boolean isBusy;
    public String lastStateChangeAt;
    public VehicleInfoResponse vehicle;
    public ActiveHoursResponse activeHoursLast24h;
}
