package com.drumigo.mobile.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for POST /api/drivers (admin creates driver + vehicle).
 */
public class DriverCreateRequest {

    public final String name;
    public final String surname;
    public final String email;
    public final String address;
    public final String phone;
    @SerializedName("profilePictureUrl")
    public final String profilePictureUrl;
    @SerializedName("vehicleTypeId")
    public final Long vehicleTypeId;
    @SerializedName("vehicleModel")
    public final String vehicleModel;
    @SerializedName("vehicleLicensePlate")
    public final String vehicleLicensePlate;
    @SerializedName("vehicleNumSeats")
    public final Integer vehicleNumSeats;
    @SerializedName("vehicleBabyFriendly")
    public final Boolean vehicleBabyFriendly;
    @SerializedName("vehiclePetFriendly")
    public final Boolean vehiclePetFriendly;

    public DriverCreateRequest(
            String name,
            String surname,
            String email,
            String address,
            String phone,
            String profilePictureUrl,
            Long vehicleTypeId,
            String vehicleModel,
            String vehicleLicensePlate,
            Integer vehicleNumSeats,
            Boolean vehicleBabyFriendly,
            Boolean vehiclePetFriendly) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.profilePictureUrl = profilePictureUrl;
        this.vehicleTypeId = vehicleTypeId;
        this.vehicleModel = vehicleModel;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleNumSeats = vehicleNumSeats;
        this.vehicleBabyFriendly = vehicleBabyFriendly != null ? vehicleBabyFriendly : false;
        this.vehiclePetFriendly = vehiclePetFriendly != null ? vehiclePetFriendly : false;
    }
}
