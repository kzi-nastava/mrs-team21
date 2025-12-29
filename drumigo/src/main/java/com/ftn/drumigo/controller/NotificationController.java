package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.dto.NotificationResponse;
import com.ftn.drumigo.mapper.NotificationMapper;
import com.ftn.drumigo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    
    @GetMapping("/users/{userId}/notifications")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Notification> notifications = notificationService.getUserNotifications(userId, pageable);
        Page<NotificationResponse> responses = notifications.map(notificationMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notificationMapper.toResponse(notification));
    }
}

