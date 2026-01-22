import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  CancelReason,
  CancelRideResult,
  RideSummaryForCancel,
  DRIVER_CANCEL_REASONS,
} from '../../../models/cancel-ride.model';

@Component({
  selector: 'app-driver-cancel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './driver-cancel.component.html',
  styleUrl: './driver-cancel.component.scss',
})
export class DriverCancelComponent {
  private fb = inject(FormBuilder);

  @Input() ride!: RideSummaryForCancel;

  @Output() cancel = new EventEmitter<CancelRideResult>();
  @Output() keepRide = new EventEmitter<void>();

  form: FormGroup = this.fb.group({
    reasonId: ['', Validators.required],
    explanation: [''],
  });

  selectedReason: CancelReason | null = null;
  reasons = DRIVER_CANCEL_REASONS;

  get canCancel(): boolean {
    // Driver can only cancel before passengers enter (not during in_progress)
    return this.ride.status === 'pending' || this.ride.status === 'driver_arriving';
  }

  get isFormValid(): boolean {
    if (!this.selectedReason) return false;
    if (this.selectedReason.requiresExplanation) {
      const explanation = this.form.get('explanation')?.value?.trim();
      return !!explanation && explanation.length >= 10;
    }
    return true;
  }

  selectReason(reason: CancelReason): void {
    this.selectedReason = reason;
    this.form.patchValue({ reasonId: reason.id });

    if (reason.requiresExplanation) {
      this.form.get('explanation')?.setValidators([Validators.required, Validators.minLength(10)]);
    } else {
      this.form.get('explanation')?.clearValidators();
    }
    this.form.get('explanation')?.updateValueAndValidity();
  }

  onKeepRide(): void {
    this.keepRide.emit();
  }

  onCancelRide(): void {
    if (!this.isFormValid || !this.selectedReason) return;

    const result: CancelRideResult = {
      reasonId: this.selectedReason.id,
      reasonLabel: this.selectedReason.label,
    };

    if (this.selectedReason.requiresExplanation) {
      result.explanation = this.form.get('explanation')?.value?.trim();
    }

    this.cancel.emit(result);
  }
}
