package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.RideInconsistency;
import com.ftn.drumigo.dto.RideInconsistencyResponse;
import org.springframework.stereotype.Component;

@Component
public class RideInconsistencyMapper {
    
    public RideInconsistencyResponse toResponse(RideInconsistency inconsistency) {
        if (inconsistency == null) {
            return null;
        }
        return new RideInconsistencyResponse(
            inconsistency.getId(),
            inconsistency.getRide() != null ? inconsistency.getRide().getId() : null,
            inconsistency.getPassenger() != null ? inconsistency.getPassenger().getId() : null,
            inconsistency.getPassenger() != null ? inconsistency.getPassenger().getName() : null,
            inconsistency.getPassenger() != null ? inconsistency.getPassenger().getSurname() : null,
            inconsistency.getNote(),
            inconsistency.getCreatedAt()
        );
    }
}

