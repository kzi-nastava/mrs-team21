package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.profile.request.ProfileUpdateRequest;
import com.ftn.drumigo.dto.profile.response.ProfileResponse;
import com.ftn.drumigo.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    
    private final ProfileService profileService;
    
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@RequestParam Long userId) {
        ProfileResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestParam Long userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
