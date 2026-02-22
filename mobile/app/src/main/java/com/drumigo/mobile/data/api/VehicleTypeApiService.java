package com.drumigo.mobile.data.api;

import com.drumigo.mobile.data.model.vehicletype.VehicleTypeResponse;
import com.drumigo.mobile.data.model.vehicletype.VehicleTypeUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface VehicleTypeApiService {

    @GET("vehicle-types")
    Call<List<VehicleTypeResponse>> getAllVehicleTypes();

    @PUT("vehicle-types/{id}")
    Call<VehicleTypeResponse> updateVehicleType(
        @Path("id") long id,
        @Body VehicleTypeUpdateRequest request
    );
}
