import { Component, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RideApiService } from '../../../services/ride-api.service';
import { ActiveRide } from '../../../models/active-ride.model';

@Component({
  selector: 'app-inconsistency-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inconsistency-report.component.html',
  styleUrl: './inconsistency-report.component.scss',
})
export class InconsistencyReportComponent {
  private readonly MODAL_CLOSE_DELAY_MS = 2000;
  private readonly MIN_NOTE_LENGTH = 10;

  private destroyRef = inject(DestroyRef);
  private rideApiService = inject(RideApiService);

  activeRide = signal<ActiveRide | null>(null);
  showModal = signal<boolean>(false);
  inconsistencyNote = signal<string>('');
  isSubmittingReport = signal<boolean>(false);
  reportSubmitted = signal<boolean>(false);
  submitError = signal<string | null>(null);

  openModal(): void {
    this.showModal.set(true);
    this.reportSubmitted.set(false);
    this.inconsistencyNote.set('');
    this.submitError.set(null);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.inconsistencyNote.set('');
    this.submitError.set(null);
  }

  submitReport(): void {
    const note = this.inconsistencyNote().trim();
    if (!note || note.length < this.MIN_NOTE_LENGTH) {
      this.submitError.set(`Please provide at least ${this.MIN_NOTE_LENGTH} characters`);
      return;
    }

    const ride = this.activeRide();
    if (!ride) {
      this.submitError.set('No active ride found');
      return;
    }

    const rideId = Number(ride.id);
    if (isNaN(rideId)) {
      this.submitError.set('Invalid ride ID');
      return;
    }

    this.isSubmittingReport.set(true);
    this.submitError.set(null);

    this.rideApiService
      .reportInconsistency(rideId, note)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.reportSubmitted.set(true);
          this.isSubmittingReport.set(false);
          const timeoutId = setTimeout(() => {
            this.closeModal();
          }, this.MODAL_CLOSE_DELAY_MS);
          this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
        },
        error: (error) => {
          console.error('Error submitting inconsistency report:', error);
          this.isSubmittingReport.set(false);
          this.submitError.set(
            error.error?.message || 'Failed to submit report. Please try again.',
          );
        },
      });
  }

  setActiveRide(ride: ActiveRide | null): void {
    this.activeRide.set(ride);
  }
}
