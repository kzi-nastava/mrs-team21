import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface PanicRideInfo {
  driverName: string;
  driverPhone?: string;
  vehicleModel: string;
  licensePlate: string;
  currentLocation: string;
  destination: string;
}

@Component({
  selector: 'app-panic-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './panic-button.component.html',
  styleUrl: './panic-button.component.scss',
})
export class PanicButtonComponent {
  @Input() rideInfo: PanicRideInfo | null = null;
  @Output() panicActivated = new EventEmitter<void>();
  @Output() contactSupport = new EventEmitter<void>();
  @Output() cancelRide = new EventEmitter<void>();

  isPressing = signal(false);
  holdProgress = signal(0);
  panicTriggered = signal(false);

  private holdTimeout: ReturnType<typeof setTimeout> | null = null;
  private progressInterval: ReturnType<typeof setInterval> | null = null;
  private readonly holdDuration = 2000; // 2 seconds to activate
  private readonly progressUpdateInterval = 50;

  onPanicPress(): void {
    if (this.panicTriggered()) return;

    this.isPressing.set(true);
    this.holdProgress.set(0);

    let elapsed = 0;
    this.progressInterval = setInterval(() => {
      elapsed += this.progressUpdateInterval;
      const progress = Math.min((elapsed / this.holdDuration) * 100, 100);
      this.holdProgress.set(progress);
    }, this.progressUpdateInterval);

    this.holdTimeout = setTimeout(() => {
      this.triggerPanic();
    }, this.holdDuration);
  }

  onPanicRelease(): void {
    if (this.panicTriggered()) return;

    this.isPressing.set(false);
    this.clearTimers();
    this.holdProgress.set(0);
  }

  private triggerPanic(): void {
    this.clearTimers();
    this.panicTriggered.set(true);
    this.holdProgress.set(100);
    this.panicActivated.emit();
  }

  private clearTimers(): void {
    if (this.holdTimeout) {
      clearTimeout(this.holdTimeout);
      this.holdTimeout = null;
    }
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  onContactSupport(): void {
    this.contactSupport.emit();
  }

  onCancelRide(): void {
    this.cancelRide.emit();
  }

  resetPanic(): void {
    this.panicTriggered.set(false);
    this.isPressing.set(false);
    this.holdProgress.set(0);
  }
}
