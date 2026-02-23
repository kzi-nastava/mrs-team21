package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.PasswordUpdateRequest;
import com.ftn.drumigo.dto.profile.request.ProfileUpdateRequest;
import com.ftn.drumigo.dto.profile.response.ProfileResponse;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.AuthService;
import com.ftn.drumigo.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private final AuthService authService;

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

    /**
     * Upload profile picture. Returns the URL path to store; client should then call PUT /api/profile with profilePictureUrl set to this value.
     */
    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("Profile picture upload: file part missing or empty");
                return ResponseEntity.badRequest().body(Map.of("error", "Profile picture file is required"));
            }
            log.debug("Profile picture upload: userId={}, size={}, contentType={}, filename={}",
                userDetails.getUserId(), file.getSize(), file.getContentType(), file.getOriginalFilename());
            String url = profileService.saveProfilePicture(userDetails.getUserId(), file);
            log.debug("Profile picture saved: url={}", url);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            log.warn("Profile picture upload rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Profile picture upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to save picture"));
        }
    }

    /**
     * Change password for the currently authenticated user (current + new password).
     * Inspired by frontend reset-password flow; uses same validation as backend AuthService.
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {
        authService.updatePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
