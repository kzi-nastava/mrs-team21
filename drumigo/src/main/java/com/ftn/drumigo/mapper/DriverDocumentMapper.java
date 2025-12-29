package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.DriverDocument;
import com.ftn.drumigo.dto.DriverDocumentResponse;
import org.springframework.stereotype.Component;

@Component
public class DriverDocumentMapper {
    
    public DriverDocumentResponse toResponse(DriverDocument document) {
        if (document == null) {
            return null;
        }
        
        return new DriverDocumentResponse(
            document.getId(),
            document.getDriver().getId(),
            document.getDocumentName(),
            document.getDocumentUrl(),
            document.getUploadedAt()
        );
    }
}
