package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Review;
import com.ftn.drumigo.dto.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    
    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        return new ReviewResponse(
            review.getId(),
            review.getRide() != null ? review.getRide().getId() : null,
            review.getPassenger() != null ? review.getPassenger().getId() : null,
            review.getPassenger() != null ? review.getPassenger().getName() : null,
            review.getPassenger() != null ? review.getPassenger().getSurname() : null,
            review.getRatingDriver(),
            review.getRatingVehicle(),
            review.getComment(),
            review.getCreatedAt()
        );
    }
}

