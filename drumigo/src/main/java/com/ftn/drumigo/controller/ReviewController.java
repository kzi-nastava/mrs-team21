package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.dto.ReviewCreateRequest;
import com.ftn.drumigo.dto.ReviewResponse;
import com.ftn.drumigo.dto.RideRatingStatusResponse;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
     * Uses the authenticated user's email from the JWT token (set as principal in JwtFilter).
     * Only passengers can create reviews.
     */
    @PostMapping("/{rideId}/reviews")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long rideId,
            Principal principal,
            @Valid @RequestBody ReviewCreateRequest request) {
        String email = extractEmail(principal);
        Review review = reviewService.createReviewByEmail(rideId, email, request);
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
     * Uses the authenticated user's email from the JWT token (set as principal in JwtFilter).
     * Returns whether the passenger can rate, if they already have a review,
     * days remaining to rate, and the rating deadline.
     * Only passengers can check rating status.
     */
    @GetMapping("/{rideId}/rating-status")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideRatingStatusResponse> getRatingStatus(
            @PathVariable Long rideId,
            Principal principal) {
        String email = extractEmail(principal);
        RideRatingStatusResponse status = reviewService.getRatingStatusByEmail(rideId, email);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Extract email from principal with null safety.
     * The email is set as the principal name in JwtFilter from JWT subject.
     */
    private String extractEmail(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new BadRequestException("Authentication required");
        }
        return principal.getName();
    }
}

