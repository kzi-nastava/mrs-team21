package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.SetPasswordRequest;
import com.ftn.drumigo.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activation")
@RequiredArgsConstructor
public class ActivationController {
    
    private final DriverService driverService;
    
    @PutMapping("/{token}/set-password")
    public ResponseEntity<Void> setPassword(
            @PathVariable String token,
            @Valid @RequestBody SetPasswordRequest request) {
        driverService.setPassword(token, request);
        return ResponseEntity.noContent().build();
    }
}

