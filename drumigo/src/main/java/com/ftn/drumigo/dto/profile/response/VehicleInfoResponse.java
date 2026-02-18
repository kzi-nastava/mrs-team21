package com.ftn.drumigo.dto.profile.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleInfoResponse {
    private Long id;
    private String model;
    private String licensePlate;
    private String vehicleTypeName;
    private Integer numSeats;
    private Boolean babyFriendly;
    private Boolean petFriendly;
}
