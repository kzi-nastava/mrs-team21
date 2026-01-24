import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DriverHistoryMockService } from './services/driver-history-mock.service';
import { RideHistory } from './models/ride-history.model';

@Component({
  selector: 'app-driver-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './driver-history.component.html',
  styleUrl: './driver-history.component.scss',
})
export class DriverHistoryComponent implements OnInit {
  allRides = signal<RideHistory[]>([]);
  filteredRides = signal<RideHistory[]>([]);
  
  startDate = signal<string>('');
  endDate = signal<string>('');

  selectedRide = signal<RideHistory | null>(null);

  constructor(private historyService: DriverHistoryMockService) {}

  ngOnInit(): void {
    const rides = this.historyService.getRideHistory();
    this.allRides.set(rides);
    this.filteredRides.set(rides);
    
    // Set default date range (last 30 days)
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    
    this.endDate.set(endDate.toISOString().split('T')[0]);
    this.startDate.set(startDate.toISOString().split('T')[0]);
  }

  onDateFilterChange(): void {
    const start = this.startDate() ? new Date(this.startDate()) : null;
    const end = this.endDate() ? new Date(this.endDate()) : null;
    
    if (!start && !end) {
      this.filteredRides.set(this.allRides());
      return;
    }

    const filtered = this.allRides().filter((ride) => {
      const rideDate = new Date(ride.startTime);
      rideDate.setHours(0, 0, 0, 0);
      
      if (start) {
        start.setHours(0, 0, 0, 0);
        if (rideDate < start) return false;
      }
      
      if (end) {
        end.setHours(23, 59, 59, 999);
        if (rideDate > end) return false;
      }
      
      return true;
    });

    this.filteredRides.set(filtered);
  }

  selectRide(ride: RideHistory): void {
    if (this.selectedRide() === ride) {
      this.selectedRide.set(null);
    } else {
      this.selectedRide.set(ride);
    }
  }

  closeDetails(): void {
    this.selectedRide.set(null);
  }

  formatDate(date: Date | null): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(date);
  }

  formatTime(date: Date | null): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }

  formatDateTimeRange(start: Date, end: Date | null): string {
    if (!end) return `${this.formatTime(start)} - —`;
    return `${this.formatTime(start)} - ${this.formatTime(end)}`;
  }

  formatCurrency(amount: number): string {
    if (amount === 0) return '—';
    return new Intl.NumberFormat('sr-RS', {
      style: 'currency',
      currency: 'RSD',
      minimumFractionDigits: 0,
    }).format(amount);
  }

  getVehicleTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      STANDARD: 'Standard',
      LUXURY: 'Luxury',
      VAN: 'Van',
    };
    return labels[type] || type;
  }

  getPassengerInitials(passenger: { firstName: string; lastName: string }): string {
    const first = passenger.firstName?.[0] || '';
    const last = passenger.lastName?.[0] || '';
    return (first + last).toUpperCase() || 'U';
  }

  getPassengerDisplayCount(count: number): string {
    return count === 1 ? '1 passenger' : `${count} passengers`;
  }

  calculateDuration(start: Date, end: Date | null): string {
    if (!end) return '—';
    const diffMs = end.getTime() - start.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    return `${diffMins} min`;
  }

  calculateDistance(origin: string, destination: string): string {
    // Mock distance calculation - in real app this would use map API
    return '3.2 km';
  }

  getRideId(ride: RideHistory): string {
    return `#RA-${ride.id.padStart(4, '0')}`;
  }
}

