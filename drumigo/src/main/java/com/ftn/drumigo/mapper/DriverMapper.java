package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Driver;
import com.ftn.drumigo.dto.DriverResponse;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {
    
    public DriverResponse toResponse(Driver driver) {
        if (driver == null) {
            return null;
        }
        
        return new DriverResponse(
            driver.getId(),
            driver.getName(),
            driver.getSurname(),
            driver.getEmail(),
            driver.getAddress(),
            driver.getPhone(),
            driver.getProfilePictureUrl(),
            driver.getBlocked(),
            driver.getActive(),
            driver.getLicenseNumber(),
            driver.getActiveDriver(),
            driver.getLastStateChangeAt(),
            driver.getCreatedAt(),
            driver.getUpdatedAt()
        );
    }
}

