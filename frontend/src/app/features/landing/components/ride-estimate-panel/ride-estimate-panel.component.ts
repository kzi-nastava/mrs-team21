import { Component, Input, Output, EventEmitter, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EstimateService } from '../../services/estimate-ride.service';
import { VehicleTypeName } from '../../models/estimate.model';

export interface RideEstimate {
  time: number; // minutes
  price: number; // RSD
  distance: number; // km
}

@Component({
  selector: 'app-ride-estimate-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './ride-estimate-panel.component.html',
  styleUrl: './ride-estimate-panel.component.scss',
})
export class RideEstimatePanelComponent {
  @Input() isOpen = false;
  @Output() panelClosed = new EventEmitter<void>();

  pickupLocation = signal('');
  destinationLocation = signal('');
  vehicleType = signal<VehicleTypeName>(VehicleTypeName.STANDARD);
  showResults = signal(false);
  isLoading = signal(false);

  estimate = signal<RideEstimate | null>(null);

  private estimateService = inject(EstimateService);

  vehicleTypes = Object.values(VehicleTypeName);

  closePanel(): void {
    this.panelClosed.emit();
  }

  calculateEstimate(): void {
    if (!this.pickupLocation() || !this.destinationLocation()) return;

    this.isLoading.set(true);
    this.estimateService
      .getEstimate(this.pickupLocation(), this.destinationLocation(), this.vehicleType())
      .subscribe({
        next: (response) => {
          this.estimate.set({
            time: response.durationInMinutes,
            price: response.estimatedPrice,
            distance: response.distanceInKm,
          });
          this.showResults.set(true);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Estimate error:', err);
          this.isLoading.set(false);
          // TODO: Handle error (e.g., show message)
        },
      });
  }

  updatePickup(value: string): void {
    this.pickupLocation.set(value);
  }

  updateDestination(value: string): void {
    this.destinationLocation.set(value);
  }
}
