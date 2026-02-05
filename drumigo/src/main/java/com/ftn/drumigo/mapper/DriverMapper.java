package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.users.Driver;
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
            driver.getActiveDriver(),
            driver.isBusy(),
            driver.getLastStateChangeAt(),
            driver.getCreatedAt(),
            driver.getUpdatedAt()
        );
    }
}

