package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.*;
import com.ftn.drumigo.dto.auth.request.LoginRequest;
import com.ftn.drumigo.dto.auth.response.LoginResponse;
import com.ftn.drumigo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reset-password/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody ResetPasswordRequestRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ResetPasswordConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }
}

