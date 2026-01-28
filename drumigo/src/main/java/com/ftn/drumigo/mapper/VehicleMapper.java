package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.dto.VehicleResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {
    
    public VehicleResponse toResponse(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleResponse(
            vehicle.getId(),
            vehicle.getDriver() != null ? vehicle.getDriver().getId() : null,
            vehicle.getDriver() != null ? vehicle.getDriver().getName() : null,
            vehicle.getDriver() != null ? vehicle.getDriver().getSurname() : null,
            vehicle.getVehicleType() != null ? vehicle.getVehicleType().getId() : null,
            vehicle.getVehicleType() != null ? vehicle.getVehicleType().getName().name() : null,
            vehicle.getNumSeats(),
            vehicle.getBabyFriendly(),
            vehicle.getPetFriendly(),
            vehicle.getCurrentLat(),
            vehicle.getCurrentLng()
        );
    }
}

