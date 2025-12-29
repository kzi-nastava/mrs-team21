package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.dto.ReviewCreateRequest;
import com.ftn.drumigo.dto.ReviewResponse;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    
    @PostMapping("/{rideId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long rideId,
            @RequestParam Long passengerId,
            @Valid @RequestBody ReviewCreateRequest request) {
        Review review = reviewService.createReview(rideId, passengerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewMapper.toResponse(review));
    }
    
    @GetMapping("/{rideId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getRideReviews(@PathVariable Long rideId) {
        List<Review> reviews = reviewService.getRideReviews(rideId);
        List<ReviewResponse> responses = reviews.stream()
            .map(reviewMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

