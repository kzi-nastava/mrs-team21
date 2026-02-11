package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.dto.ReviewCreateRequest;
import com.ftn.drumigo.dto.ReviewResponse;
import com.ftn.drumigo.dto.RideRatingStatusResponse;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    
    /**
     * Create a review for a ride.
     * Uses the authenticated user's ID from the JWT token.
     * Only passengers can create reviews.
     */
    @PostMapping("/{rideId}/reviews")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long rideId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request) {
        Review review = reviewService.createReview(rideId, userDetails.getUserId(), request);
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
    
    /**
     * Get the rating status for a ride.
     * Uses the authenticated user's ID from the JWT token.
     * Returns whether the passenger can rate, if they already have a review,
     * days remaining to rate, and the rating deadline.
     * Only passengers can check rating status.
     */
    @GetMapping("/{rideId}/rating-status")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideRatingStatusResponse> getRatingStatus(
            @PathVariable Long rideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RideRatingStatusResponse status = reviewService.getRatingStatus(rideId, userDetails.getUserId());
        return ResponseEntity.ok(status);
    }
}

