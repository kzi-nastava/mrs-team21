import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PassengerCancelComponent } from './passenger-cancel/passenger-cancel.component';
import { DriverCancelComponent } from './driver-cancel/driver-cancel.component';
import { CancelRideResult, RideSummaryForCancel } from '../../models/cancel-ride.model';

export type CancelUserRole = 'passenger' | 'driver';

@Component({
  selector: 'app-cancel-ride',
  standalone: true,
  imports: [CommonModule, PassengerCancelComponent, DriverCancelComponent],
  template: `
    @if (userRole === 'passenger') {
      <app-passenger-cancel [ride]="ride" (cancel)="onCancel($event)" (keepRide)="onKeepRide()" />
    } @else {
      <app-driver-cancel [ride]="ride" (cancel)="onCancel($event)" (keepRide)="onKeepRide()" />
    }
  `,
})
export class CancelRideComponent {
  @Input() ride!: RideSummaryForCancel;
  @Input() userRole: CancelUserRole = 'passenger';

  @Output() cancel = new EventEmitter<CancelRideResult>();
  @Output() keepRide = new EventEmitter<void>();

  onCancel(result: CancelRideResult): void {
    this.cancel.emit(result);
  }

  onKeepRide(): void {
    this.keepRide.emit();
  }
}
