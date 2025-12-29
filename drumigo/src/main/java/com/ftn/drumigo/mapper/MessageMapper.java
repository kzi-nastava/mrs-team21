package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Message;
import com.ftn.drumigo.dto.SupportMessageResponse;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {
    
    public SupportMessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }
        return new SupportMessageResponse(
            message.getId(),
            message.getSender() != null ? message.getSender().getId() : null,
            message.getSender() != null ? message.getSender().getName() : null,
            message.getSender() != null ? message.getSender().getSurname() : null,
            message.getReceiver() != null ? message.getReceiver().getId() : null,
            message.getReceiver() != null ? message.getReceiver().getName() : null,
            message.getReceiver() != null ? message.getReceiver().getSurname() : null,
            message.getContent(),
            message.getCreatedAt()
        );
    }
}

