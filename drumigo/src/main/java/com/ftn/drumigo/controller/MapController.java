package com.ftn.drumigo.controller;

import com.ftn.drumigo.service.MapService;

import jakarta.validation.Valid;

import com.ftn.drumigo.dto.ride.request.EstimateRequest;
import com.ftn.drumigo.dto.ride.response.EstimateResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/rides")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @PostMapping("/estimate")
    public ResponseEntity<EstimateResponse> estimateRide(@Valid @RequestBody EstimateRequest request) {
        
        EstimateResponse estimate = mapService.estimateRide(request);
        return ResponseEntity.ok(estimate);
    }
    
}