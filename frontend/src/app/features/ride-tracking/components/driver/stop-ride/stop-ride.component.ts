import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

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
  imports: [CommonModule],
  templateUrl: './stop-ride.component.html',
  styleUrl: './stop-ride.component.scss',
})
export class StopRideComponent {
  private _stopInfo: StopRideInfo | null = null;
  @Input()
  set stopInfo(v: StopRideInfo | null) {
    this._stopInfo = v;
    if (v) {
      this.stopAddress.set(v.currentLocation ?? '');
    }
  }
  get stopInfo(): StopRideInfo | null {
    return this._stopInfo;
  }

  /** Emitted when driver confirms a stop. Payload: stop address string. */
  @Output() confirmStop = new EventEmitter<string>();
  @Output() continueRide = new EventEmitter<void>();

  /** Parent can bind to this to reflect submission state */
  @Input() submitting = false;

  isStopping = signal(false);

  stopAddress = signal('');

  onStopRide(): void {
    // Use the editable address and emit to parent to perform the API call
    const address = (this.stopAddress() || '').trim();
    if (!address) {
      // Basic guard; in real UI show validation error
      return;
    }
    this.isStopping.set(true);
    this.confirmStop.emit(address);
  }

  onContinueRide(): void {
    this.continueRide.emit();
  }
}
