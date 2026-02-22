package com.drumigo.mobile.data.model.vehicletype;

import com.google.gson.annotations.SerializedName;

public class VehicleTypeUpdateRequest {
    @SerializedName("startPrice")
    public double startPrice;

    @SerializedName("pricePerKm")
    public double pricePerKm;

    public VehicleTypeUpdateRequest(double startPrice, double pricePerKm) {
        this.startPrice = startPrice;
        this.pricePerKm = pricePerKm;
    }
}
