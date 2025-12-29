package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.dto.DriverRideHistoryItemResponse;
import com.ftn.drumigo.dto.ReviewResponse;
import com.ftn.drumigo.mapper.DriverRideHistoryMapper;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.service.ReviewService;
import com.ftn.drumigo.service.RideService;
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
    
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final RideService rideService;
    private final DriverRideHistoryMapper driverRideHistoryMapper;
    
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
}

