import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CancelRideResult, RideSummaryForCancel } from '../../../models/cancel-ride.model';

@Component({
  selector: 'app-passenger-cancel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passenger-cancel.component.html',
  styleUrl: './passenger-cancel.component.scss',
})
export class PassengerCancelComponent {
  @Input() ride!: RideSummaryForCancel;

  @Output() cancel = new EventEmitter<CancelRideResult>();
  @Output() keepRide = new EventEmitter<void>();

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('cancel-dialog-overlay')) {
      this.keepRide.emit();
    }
  }

  onKeepRide(): void {
    this.keepRide.emit();
  }

  onCancelRide(): void {
    const result: CancelRideResult = {
      reasonId: 'passenger_cancel',
      reasonLabel: 'Passenger cancelled ride',
    };
    this.cancel.emit(result);
  }
}
