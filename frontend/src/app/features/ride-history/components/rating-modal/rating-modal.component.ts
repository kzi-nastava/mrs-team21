import {
  Component,
  input,
  output,
  signal,
  inject,
  ChangeDetectionStrategy,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { StarRatingComponent } from '../../../../shared/components/star-rating/star-rating.component';
import { ToastService } from '../../../../shared/services/toast.service';
import { ReviewService, ReviewRequest, ReviewResponse } from '../../services/review.service';

/**
 * Ride data needed for the rating modal
 */
export interface RideForRating {
  id: string;
  driverName: string;
  driverPhotoUrl?: string;
  vehicleModel?: string;
  vehicleLicensePlate?: string;
  origin: string;
  destination: string;
  completedAt?: Date;
}

/**
 * Modal component for rating a completed ride.
 * Allows rating the driver and vehicle separately with an optional comment.
 */
@Component({
  selector: 'app-rating-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StarRatingComponent],
  templateUrl: './rating-modal.component.html',
  styleUrl: './rating-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RatingModalComponent {
  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  /** Ride data for display and submission */
  ride = input.required<RideForRating>();

  /** Emits when the modal should be closed */
  close = output<void>();

  /** Emits when a review is successfully submitted */
  submitted = output<ReviewResponse>();

  /** Loading state during submission */
  isSubmitting = signal<boolean>(false);

  /** Form for the review */
  ratingForm = new FormGroup({
    driverRating: new FormControl<number>(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),
    vehicleRating: new FormControl<number>(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),
    comment: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.maxLength(500)],
    }),
  });

  /** Close the modal */
  onClose(): void {
    if (!this.isSubmitting()) {
      this.close.emit();
    }
  }

  /** Handle backdrop click */
  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.onClose();
    }
  }

  /** Submit the review */
  onSubmit(): void {
    if (this.ratingForm.invalid || this.isSubmitting()) {
      // Mark all fields as touched to show validation errors
      this.ratingForm.markAllAsTouched();
      return;
    }

    const request: ReviewRequest = {
      ratingDriver: this.ratingForm.value.driverRating!,
      ratingVehicle: this.ratingForm.value.vehicleRating!,
      comment: this.ratingForm.value.comment || undefined,
    };

    this.isSubmitting.set(true);

    this.reviewService
      .submitReview(this.ride().id, request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (review) => {
          this.toastService.success('Thank you for your feedback!');
          this.submitted.emit(review);
          this.close.emit();
        },
        error: (error) => {
          console.error('Failed to submit review:', error);
          const message = error.error?.message || 'Failed to submit review. Please try again.';
          this.toastService.error(message);
        },
      });
  }

  /** Check if driver rating has error */
  get driverRatingInvalid(): boolean {
    const control = this.ratingForm.get('driverRating');
    return !!(control?.touched && control?.invalid);
  }

  /** Check if vehicle rating has error */
  get vehicleRatingInvalid(): boolean {
    const control = this.ratingForm.get('vehicleRating');
    return !!(control?.touched && control?.invalid);
  }

  /** Get remaining characters for comment */
  get commentCharsRemaining(): number {
    return 500 - (this.ratingForm.value.comment?.length || 0);
  }
}
