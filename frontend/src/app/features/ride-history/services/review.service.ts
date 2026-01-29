import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

/**
 * Request DTO for creating a review
 */
export interface ReviewRequest {
  ratingDriver: number;
  ratingVehicle: number;
  comment?: string;
}

/**
 * Response DTO for a review
 */
export interface ReviewResponse {
  id: number;
  rideId: number;
  passengerId: number;
  passengerName: string;
  ratingDriver: number;
  ratingVehicle: number;
  comment: string | null;
  createdAt: string;
}

/**
 * Response DTO for rating status
 */
export interface RatingStatus {
  canRate: boolean;
  hasReview: boolean;
  daysRemaining: number;
  ratingDeadline: string | null;
}

/**
 * Service for managing ride reviews.
 * Handles creating reviews and checking rating eligibility.
 */
@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  /**
   * Submit a review for a ride.
   * Uses the authenticated user's JWT token for identification.
   */
  submitReview(rideId: string, request: ReviewRequest): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.baseUrl}/rides/${rideId}/reviews`, request);
  }

  /**
   * Get the rating status for a ride.
   * Returns whether the user can rate, days remaining, etc.
   */
  getRatingStatus(rideId: string): Observable<RatingStatus> {
    return this.http.get<RatingStatus>(`${this.baseUrl}/rides/${rideId}/rating-status`);
  }

  /**
   * Get all reviews for a ride.
   */
  getRideReviews(rideId: string): Observable<ReviewResponse[]> {
    return this.http.get<ReviewResponse[]>(`${this.baseUrl}/rides/${rideId}/reviews`);
  }
}
