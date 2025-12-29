package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Admin;
import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.PanicEvent;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.User;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.PanicCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.AdminRepository;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.PanicEventRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PanicService {
    
    private final PanicEventRepository panicEventRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final NotificationRepository notificationRepository;
    
    public PanicEvent create(Long rideId, Long userId, PanicCreateRequest request) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        
        // Validate that ride is in a valid state for panic (should be ACTIVE)
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new BadRequestException("Panic can only be triggered for ACTIVE rides. Current status: " + ride.getStatus());
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Create panic event
        PanicEvent panicEvent = new PanicEvent();
        panicEvent.setRide(ride);
        panicEvent.setUser(user);
        panicEvent.setReason(request.reason());
        panicEvent.setCreatedAt(Instant.now());
        
        panicEvent = panicEventRepository.save(panicEvent);
        
        // Create notification for all admins (spec 2.6.3)
        List<Admin> admins = adminRepository.findAll();
        for (Admin admin : admins) {
            Notification notification = new Notification();
            notification.setUser(admin);
            notification.setRide(ride);
            notification.setType(NotificationType.PANIC_ALERT);
            notification.setMessage("Panic alert: Ride #" + ride.getId() + " - " + request.reason());
            notificationRepository.save(notification);
        }
        
        return panicEvent;
    }
    
    public Page<PanicEvent> getAll(Pageable pageable) {
        return panicEventRepository.findAll(pageable);
    }
}

