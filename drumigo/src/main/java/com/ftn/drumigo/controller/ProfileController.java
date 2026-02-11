package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.profile.request.ProfileUpdateRequest;
import com.ftn.drumigo.dto.profile.response.ProfileResponse;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    
    private final ProfileService profileService;
    
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ProfileResponse response = profileService.getProfile(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileResponse response = profileService.updateProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
