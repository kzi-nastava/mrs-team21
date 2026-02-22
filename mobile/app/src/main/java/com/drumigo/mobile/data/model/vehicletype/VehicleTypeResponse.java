package com.drumigo.mobile.data.model.vehicletype;

import com.google.gson.annotations.SerializedName;

public class VehicleTypeResponse {
    @SerializedName("id")
    public Long id;

    @SerializedName("name")
    public String name;

    @SerializedName("startPrice")
    public Double startPrice;

    @SerializedName("pricePerKm")
    public Double pricePerKm;
}
