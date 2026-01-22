package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Driver;
import com.ftn.drumigo.domain.DriverDocument;
import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.dto.*;
import com.ftn.drumigo.mapper.DriverDocumentMapper;
import com.ftn.drumigo.mapper.DriverMapper;
import com.ftn.drumigo.mapper.DriverRideHistoryMapper;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.service.DriverService;
import com.ftn.drumigo.service.ReviewService;
import com.ftn.drumigo.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final DriverService driverService;
    private final DriverMapper driverMapper;
    private final DriverDocumentMapper driverDocumentMapper;
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final RideService rideService;
    private final DriverRideHistoryMapper driverRideHistoryMapper;
    
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody DriverCreateRequest request) {
        Driver driver = driverService.create(request);
        return ResponseEntity.status(201).body(driverMapper.toResponse(driver));
    }
    
    @GetMapping
    public ResponseEntity<Page<DriverResponse>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Driver> drivers = driverService.getAll(pageable);
        Page<DriverResponse> responses = drivers.map(driverMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        Driver driver = driverService.getById(id);
        return ResponseEntity.ok(driverMapper.toResponse(driver));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverUpdateRequest request) {
        Driver driver = driverService.update(id, request);
        return ResponseEntity.ok(driverMapper.toResponse(driver));
    }
    
    @PostMapping("/{id}/documents")
    public ResponseEntity<DriverDocumentResponse> createDocument(
            @PathVariable Long id,
            @Valid @RequestBody DriverDocumentCreateRequest request) {
        DriverDocument document = driverService.createDocument(id, request);
        return ResponseEntity.status(201).body(driverDocumentMapper.toResponse(document));
    }
    
    @PostMapping("/{id}/activation")
    public ResponseEntity<ActivationTokenResponse> createActivationToken(@PathVariable Long id) {
        String token = driverService.createActivationToken(id);
        // Token expires in 24 hours
        Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
        return ResponseEntity.ok(new ActivationTokenResponse(token, expiresAt));
    }
    
    @GetMapping("/{driverId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getDriverReviews(@PathVariable Long driverId) {
        List<Review> reviews = reviewService.getDriverReviews(driverId);
        List<ReviewResponse> responses = reviews.stream()
            .map(reviewMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{driverId}/rides/history")
    public ResponseEntity<Page<DriverRideHistoryItemResponse>> getDriverRideHistory(
            @PathVariable Long driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt,desc") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Ride> rides = rideService.getDriverRideHistory(driverId, from, to, pageable);
        Page<DriverRideHistoryItemResponse> responses = rides.map(driverRideHistoryMapper::toResponse);
        
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}/state")
    public ResponseEntity<DriverResponse> updateDriverState(
            @PathVariable Long id,
            @Valid @RequestBody DriverStateUpdateRequest request) {
        Driver driver = driverService.updateDriverState(id, request.activeDriver());
        return ResponseEntity.ok(driverMapper.toResponse(driver));
    }
}

