package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.User;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setReadAt(Instant.now());
        return notificationRepository.save(notification);
    }
}

