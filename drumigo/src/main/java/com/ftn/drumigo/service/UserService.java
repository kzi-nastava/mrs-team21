package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.UserUpdateRequest;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    
    public User update(Long id, UserUpdateRequest request) {
        User user = getById(id);
        
        // Check email uniqueness if changed
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("User with email " + request.email() + " already exists");
            }
            user.setEmail(request.email());
        }
        
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.surname() != null) {
            user.setSurname(request.surname());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.profilePictureUrl() != null) {
            user.setProfilePictureUrl(request.profilePictureUrl());
        }
        
        user.setUpdatedAt(Instant.now());
        
        return userRepository.save(user);
    }
    
    public User block(Long id) {
        User user = getById(id);
        user.setBlocked(true);
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }
    
    public User unblock(Long id) {
        User user = getById(id);
        user.setBlocked(false);
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }
    
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}

