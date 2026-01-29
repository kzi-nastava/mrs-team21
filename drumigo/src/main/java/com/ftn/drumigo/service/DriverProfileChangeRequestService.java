package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.users.Admin;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.DriverProfileChangeRequest;
import com.ftn.drumigo.domain.enums.RequestStatus;
import com.ftn.drumigo.dto.DriverProfileChangeRequestCreateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.AdminRepository;
import com.ftn.drumigo.repository.DriverProfileChangeRequestRepository;
import com.ftn.drumigo.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverProfileChangeRequestService {
    
    private final DriverProfileChangeRequestRepository requestRepository;
    private final DriverRepository driverRepository;
    private final AdminRepository adminRepository;
    
    // Simple JSON value extractor for KT1 (for production, use a proper JSON library like Jackson)
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public DriverProfileChangeRequest create(Long driverId, DriverProfileChangeRequestCreateRequest request) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        
        DriverProfileChangeRequest changeRequest = new DriverProfileChangeRequest();
        changeRequest.setDriver(driver);
        changeRequest.setRequestedChangesJson(request.requestedChangesJson());
        changeRequest.setStatus(RequestStatus.PENDING);
        changeRequest.setCreatedAt(Instant.now());
        
        return requestRepository.save(changeRequest);
    }
    
    public Page<DriverProfileChangeRequest> getPendingRequests(Pageable pageable) {
        return requestRepository.findByStatus(RequestStatus.PENDING, pageable);
    }
    
    public DriverProfileChangeRequest approve(Long requestId, Long adminId) {
        DriverProfileChangeRequest changeRequest = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Profile change request not found with id: " + requestId));
        
        if (changeRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending. Current status: " + changeRequest.getStatus());
        }
        
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        
        changeRequest.setStatus(RequestStatus.APPROVED);
        changeRequest.setReviewedAt(Instant.now());
        changeRequest.setReviewedByAdmin(admin);
        
        // Apply changes from JSON to driver profile
        Driver driver = changeRequest.getDriver();
        String json = changeRequest.getRequestedChangesJson();
        
        try {
            // Extract and update driver fields from JSON (simple regex-based parser for KT1)
            // For production, use a proper JSON library like Jackson ObjectMapper
            String name = extractJsonValue(json, "name");
            if (name != null && !name.isEmpty()) {
                driver.setName(name);
            }
            
            String surname = extractJsonValue(json, "surname");
            if (surname != null && !surname.isEmpty()) {
                driver.setSurname(surname);
            }
            
            String address = extractJsonValue(json, "address");
            if (address != null) {
                driver.setAddress(address);
            }
            
            String phone = extractJsonValue(json, "phone");
            if (phone != null) {
                driver.setPhone(phone);
            }
            
            String profilePictureUrl = extractJsonValue(json, "profilePictureUrl");
            if (profilePictureUrl != null) {
                driver.setProfilePictureUrl(profilePictureUrl);
            }
            
            // licenseNumber field removed from Driver entity
            
            // Note: email changes should be handled separately with uniqueness checks
            // For KT1, we skip email updates via profile change requests
            
            driver.setUpdatedAt(Instant.now());
            driverRepository.save(driver);
        } catch (Exception e) {
            throw new BadRequestException("Error applying requested changes: " + e.getMessage());
        }
        
        return requestRepository.save(changeRequest);
    }
    
    public DriverProfileChangeRequest reject(Long requestId, Long adminId) {
        DriverProfileChangeRequest changeRequest = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Profile change request not found with id: " + requestId));
        
        if (changeRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request is not pending. Current status: " + changeRequest.getStatus());
        }
        
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        
        changeRequest.setStatus(RequestStatus.REJECTED);
        changeRequest.setReviewedAt(Instant.now());
        changeRequest.setReviewedByAdmin(admin);
        
        return requestRepository.save(changeRequest);
    }
}

