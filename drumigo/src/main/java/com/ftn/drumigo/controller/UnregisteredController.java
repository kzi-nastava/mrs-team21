package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.EstimateRequest;
import com.ftn.drumigo.dto.EstimateResponse;
import com.ftn.drumigo.service.UnregisteredService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class UnregisteredController {
    
    private final UnregisteredService unregisteredService;
    
    @PostMapping("/estimate")
    public ResponseEntity<EstimateResponse> getEstimate(@Valid @RequestBody EstimateRequest request) {
        EstimateResponse response = unregisteredService.getEstimate(request);
        return ResponseEntity.ok(response);
    }
}

