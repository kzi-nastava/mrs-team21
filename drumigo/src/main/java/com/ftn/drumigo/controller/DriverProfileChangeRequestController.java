package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.DriverProfileChangeRequest;
import com.ftn.drumigo.dto.DriverProfileChangeRequestCreateRequest;
import com.ftn.drumigo.service.DriverProfileChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverProfileChangeRequestController {
    
    private final DriverProfileChangeRequestService profileChangeRequestService;
    private final com.ftn.drumigo.mapper.DriverProfileChangeRequestMapper profileChangeRequestMapper;
    
    @PostMapping("/{id}/profile-change-requests")
    public ResponseEntity<com.ftn.drumigo.dto.DriverProfileChangeRequestResponse> createProfileChangeRequest(
            @PathVariable Long id,
            @Valid @RequestBody DriverProfileChangeRequestCreateRequest request) {
        DriverProfileChangeRequest changeRequest = profileChangeRequestService.create(id, request);
        return ResponseEntity.status(201).body(profileChangeRequestMapper.toResponse(changeRequest));
    }
}

