import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DecimalPipe } from '@angular/common';

export interface StopRideInfo {
  passengerName: string;
  passengerRating: number;
  ridesDone: number;
  pickupLocation: string;
  destinationLocation: string;
  currentLocation: string;
  duration: string;
  distance: string;
  completionPercent: number;
  originalFare: number;
  adjustedFare: number;
  currency: string;
}

@Component({
  selector: 'app-stop-ride',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './stop-ride.component.html',
  styleUrl: './stop-ride.component.scss',
})
export class StopRideComponent {
  @Input() stopInfo: StopRideInfo | null = null;
  @Output() stopRide = new EventEmitter<void>();
  @Output() continueRide = new EventEmitter<void>();

  isStopping = signal(false);

  onStopRide(): void {
    this.isStopping.set(true);
    // Simulate processing
    setTimeout(() => {
      this.stopRide.emit();
    }, 500);
  }

  onContinueRide(): void {
    this.continueRide.emit();
  }
}
