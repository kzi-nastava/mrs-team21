package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.DriverProfileChangeRequest;
import com.ftn.drumigo.dto.DriverProfileChangeRequestResponse;
import org.springframework.stereotype.Component;

@Component
public class DriverProfileChangeRequestMapper {
    
    public DriverProfileChangeRequestResponse toResponse(DriverProfileChangeRequest request) {
        if (request == null) {
            return null;
        }
        
        return new DriverProfileChangeRequestResponse(
            request.getId(),
            request.getDriver().getId(),
            request.getDriver().getName(),
            request.getDriver().getSurname(),
            request.getRequestedChangesJson(),
            request.getStatus(),
            request.getCreatedAt(),
            request.getReviewedAt(),
            request.getReviewedByAdmin() != null ? request.getReviewedByAdmin().getId() : null
        );
    }
}

