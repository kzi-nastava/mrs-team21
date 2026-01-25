import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RideTrackingMockService } from '../../../services/ride-tracking-mock.service';
import { ActiveRide } from '../../../models/active-ride.model';

@Component({
  selector: 'app-inconsistency-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inconsistency-report.component.html',
  styleUrl: './inconsistency-report.component.scss',
})
export class InconsistencyReportComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private rideTrackingService = inject(RideTrackingMockService);

  activeRide = signal<ActiveRide | null>(null);
  showModal = signal<boolean>(false);
  inconsistencyNote = signal<string>('');
  isSubmittingReport = signal<boolean>(false);
  reportSubmitted = signal<boolean>(false);

  ngOnInit(): void {}

  openModal(): void {
    this.showModal.set(true);
    this.reportSubmitted.set(false);
    this.inconsistencyNote.set('');
  }

  closeModal(): void {
    this.showModal.set(false);
    this.inconsistencyNote.set('');
  }

  submitReport(): void {
    const note = this.inconsistencyNote().trim();
    if (!note || note.length < 10) {
      return; // Basic validation
    }

    const ride = this.activeRide();
    if (!ride) return;

    this.isSubmittingReport.set(true);

    // Mock user data - in real app, get from auth service
    const reportedBy = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
    };

    this.rideTrackingService
      .reportInconsistency(ride.id, note, reportedBy)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.reportSubmitted.set(true);
          this.isSubmittingReport.set(false);
          const timeoutId = setTimeout(() => {
            this.closeModal();
          }, 2000);
          this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
        },
        error: (error) => {
          console.error('Error submitting report:', error);
          this.isSubmittingReport.set(false);
        },
      });
  }

  setActiveRide(ride: ActiveRide | null): void {
    this.activeRide.set(ride);
  }
}
