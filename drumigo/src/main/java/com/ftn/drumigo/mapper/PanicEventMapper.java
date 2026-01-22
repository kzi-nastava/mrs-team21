package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.PanicEvent;
import com.ftn.drumigo.dto.PanicEventResponse;
import org.springframework.stereotype.Component;

@Component
public class PanicEventMapper {
    
    public PanicEventResponse toResponse(PanicEvent panicEvent) {
        return new PanicEventResponse(
            panicEvent.getId(),
            panicEvent.getRide().getId(),
            panicEvent.getUser().getId(),
            panicEvent.getUser().getEmail(),
            panicEvent.getReason(),
            panicEvent.getCreatedAt()
        );
    }
}

