package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.UserNote;
import com.ftn.drumigo.dto.UserNoteResponse;
import org.springframework.stereotype.Component;

@Component
public class UserNoteMapper {
    
    public UserNoteResponse toResponse(UserNote note) {
        if (note == null) {
            return null;
        }
        
        return new UserNoteResponse(
            note.getId(),
            note.getUser().getId(),
            note.getAdmin().getId(),
            note.getAdmin().getName(),
            note.getNote(),
            note.getCreatedAt()
        );
    }
}

