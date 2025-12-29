package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.dto.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    
    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationResponse(
            notification.getId(),
            notification.getUser() != null ? notification.getUser().getId() : null,
            notification.getRide() != null ? notification.getRide().getId() : null,
            notification.getType().name(),
            notification.getMessage(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}

