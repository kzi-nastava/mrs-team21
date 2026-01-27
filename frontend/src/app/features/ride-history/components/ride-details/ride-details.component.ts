import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../models';

export interface RideDetailsConfig {
  showPassengers: boolean;
  showDriverInfo: boolean;
  showPanicAlert: boolean;
  showCancellationInfo: boolean;
  showEarnings: boolean;
  showRatings: boolean;
}

/**
 * Reusable ride details component.
 * Presentational only - displays ride information in a side panel.
 * Visibility of sections is controlled via config input.
 */
@Component({
  selector: 'app-ride-details',
  standalone: true,
  imports: [CommonModule],
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
  });

  close = output<void>();

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
}
