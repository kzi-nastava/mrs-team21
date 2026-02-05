package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.VehicleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface VehicleApiService {

    @GET("vehicles/active")
    Call<List<VehicleResponse>> getActiveVehicles();
}
