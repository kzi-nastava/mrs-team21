import { Component, input, output, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../models';
import { StarRatingComponent } from '../../../../shared/components/star-rating/star-rating.component';

export interface RideDetailsConfig {
  showPassengers: boolean;
  showDriverInfo: boolean;
  showPanicAlert: boolean;
  showCancellationInfo: boolean;
  showEarnings: boolean;
  showRatings: boolean;
  /** Show the rating action button (for passengers only) */
  showRatingAction: boolean;
  /** Show "Track ride" button for in-progress (ACTIVE) rides (e.g. admin) */
  showTrackRideButton?: boolean;
}

/**
 * Reusable ride details component.
 * Presentational only - displays ride information in a side panel.
 * Visibility of sections is controlled via config input.
 */
@Component({
  selector: 'app-ride-details',
  standalone: true,
  imports: [CommonModule, StarRatingComponent],
  templateUrl: './ride-details.component.html',
  styleUrl: './ride-details.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RideDetailsComponent {
  ride = input<Ride | null>(null);
  config = input<RideDetailsConfig>({
    showPassengers: true,
    showDriverInfo: false,
    showPanicAlert: true,
    showCancellationInfo: true,
    showEarnings: true,
    showRatings: true,
    showRatingAction: false,
  });

  close = output<void>();

  /** Emits when user wants to track the ride (admin: navigate to ride-tracking page) */
  trackRide = output<Ride>();

  /** Emits when user wants to rate the ride */
  rateRide = output<Ride>();

  /** Check if the ride can be rated (within 3 day deadline and not yet rated) */
  canRateRide = computed(() => {
    const ride = this.ride();
    if (!ride) return false;
    if (ride.isCancelled) return false;
    if (ride.hasReview) return false;
    if (!ride.canRate) return false;
    return true;
  });

  /** Get days remaining to rate */
  daysRemaining = computed(() => {
    const ride = this.ride();
    return ride?.daysRemainingToRate ?? 0;
  });

  formatDate(date: Date | null): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(date);
  }

  formatTime(date: Date | null): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }

  formatCurrency(amount: number): string {
    if (amount === 0) return '—';
    return new Intl.NumberFormat('sr-RS', {
      style: 'currency',
      currency: 'RSD',
      minimumFractionDigits: 0,
    }).format(amount);
  }

  calculateDuration(start: Date, end: Date | null): string {
    if (!end) return '—';
    const diffMs = end.getTime() - start.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    return `${diffMins} min`;
  }

  calculateDistance(_origin: string, _destination: string): string {
    // TODO: Mock distance calculation - in real app this would use map API
    return '3.2 km';
  }

  getRideId(ride: Ride): string {
    return `#RA-${ride.id.padStart(4, '0')}`;
  }

  onClose(): void {
    this.close.emit();
  }

  onRateClick(): void {
    const ride = this.ride();
    if (ride && this.canRateRide()) {
      this.rateRide.emit(ride);
    }
  }

  onTrackRideClick(ride: Ride): void {
    this.trackRide.emit(ride);
  }
}
