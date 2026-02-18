package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.dto.profile.request.ProfileUpdateRequest;
import com.ftn.drumigo.dto.profile.response.ProfileResponse;
import com.ftn.drumigo.dto.profile.response.VehicleInfoResponse;
import com.ftn.drumigo.exception.ConflictException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {
    
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setSurname(user.getSurname());
        response.setEmail(user.getEmail());
        response.setAddress(user.getAddress());
        response.setPhone(user.getPhone());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setBlocked(user.getBlocked());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        // Add driver-specific fields if user is a driver
        if (user instanceof Driver driver) {
            response.setActiveDriver(driver.getActiveDriver());
            response.setIsBusy(driver.isBusy());
            response.setLastStateChangeAt(driver.getLastStateChangeAt());
            
            // Add vehicle info if driver has a vehicle
            Vehicle vehicle = vehicleRepository.findByDriver(driver).orElse(null);
            if (vehicle != null) {
                VehicleInfoResponse vehicleInfo = new VehicleInfoResponse();
                vehicleInfo.setId(vehicle.getId());
                vehicleInfo.setModel(vehicle.getModel());
                vehicleInfo.setLicensePlate(vehicle.getLicensePlate());
                vehicleInfo.setVehicleTypeName(vehicle.getVehicleType().getName().name());
                vehicleInfo.setNumSeats(vehicle.getNumSeats());
                vehicleInfo.setBabyFriendly(vehicle.getBabyFriendly());
                vehicleInfo.setPetFriendly(vehicle.getPetFriendly());
                response.setVehicle(vehicleInfo);
            }
        }
        
        return response;
    }
    
    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check email uniqueness if changed
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("User with email " + request.email() + " already exists");
            }
            user.setEmail(request.email());
        }
        
        // Update fields if provided
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
        userRepository.save(user);
        
        // Return updated profile
        return getProfile(userId);
    }
}
