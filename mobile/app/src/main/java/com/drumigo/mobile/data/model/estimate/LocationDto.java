package com.drumigo.mobile.data.model.estimate;

public class LocationDto {
    public Double latitude;
    public Double longitude;
    public String address;

    public LocationDto(Double latitude, Double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }
}
