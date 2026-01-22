package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.dto.VehicleTypeResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleTypeMapper {
    
    public VehicleTypeResponse toResponse(VehicleType vehicleType) {
        if (vehicleType == null) {
            return null;
        }
        return new VehicleTypeResponse(
            vehicleType.getId(),
            vehicleType.getName().name(),
            vehicleType.getStartPrice(),
            vehicleType.getPricePerKm()
        );
    }
}

