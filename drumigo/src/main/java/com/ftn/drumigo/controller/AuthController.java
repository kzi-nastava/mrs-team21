package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.auth.request.LoginRequest;
import com.ftn.drumigo.dto.auth.response.LoginResponse;
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

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody String email) {
        authService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password/{token}")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String token,
            @RequestBody String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok().build();
    }

    /**
     * Upload a temporary profile picture for registration (no auth required).
     * Returns the URL to send as profilePictureUrl when registering.
     */
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePictureForRegistration(@RequestParam("file") MultipartFile file) {
        try {
            String url = profileService.saveTempProfilePicture(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to save picture"));
        }
    }
}

