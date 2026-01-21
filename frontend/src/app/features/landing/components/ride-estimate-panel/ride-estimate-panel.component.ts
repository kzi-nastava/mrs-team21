import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

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
  showResults = signal(false);

  estimate: RideEstimate = {
    time: 12,
    price: 450,
    distance: 3.2,
  };

  closePanel(): void {
    this.panelClosed.emit();
  }

  calculateEstimate(): void {
    // Mock calculation - in real app this would call a service
    if (this.pickupLocation() && this.destinationLocation()) {
      // Simulate random estimate
      this.estimate = {
        time: Math.floor(Math.random() * 20) + 5,
        price: Math.floor(Math.random() * 500) + 200,
        distance: Math.round((Math.random() * 10 + 1) * 10) / 10,
      };
      this.showResults.set(true);
    }
  }

  updatePickup(value: string): void {
    this.pickupLocation.set(value);
  }

  updateDestination(value: string): void {
    this.destinationLocation.set(value);
  }
}
