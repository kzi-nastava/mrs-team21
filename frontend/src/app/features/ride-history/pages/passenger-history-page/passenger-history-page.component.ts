import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideHistoryService } from '../../services/ride-history.service';
import { Ride } from '../../models';
import { passengerHistoryConfig } from '../../config/passenger-history.config';
import { RideHistoryTableComponent, SortState } from '../../components/ride-history-table/ride-history-table.component';
import { RideHistoryFiltersComponent } from '../../components/ride-history-filters/ride-history-filters.component';
import { RideDetailsComponent } from '../../components/ride-details/ride-details.component';

/**
 * Smart page component for passenger ride history.
 * Handles data fetching and state management for passenger role.
 * Uses shared presentational components configured for passenger view.
 */
@Component({
  selector: 'app-passenger-history-page',
  standalone: true,
  imports: [
    CommonModule,
    RideHistoryTableComponent,
    RideHistoryFiltersComponent,
    RideDetailsComponent,
  ],
  templateUrl: './passenger-history-page.component.html',
  styleUrl: './passenger-history-page.component.scss',
})
export class PassengerHistoryPageComponent implements OnInit {
  allRides = signal<Ride[]>([]);
  filteredRides = signal<Ride[]>([]);
  startDate = signal<string>('');
  endDate = signal<string>('');
  selectedRide = signal<Ride | null>(null);
  currentSort = signal<SortState>({ field: null, order: 'asc' });

  config = passengerHistoryConfig;

  constructor(private rideHistoryService: RideHistoryService) {}

  ngOnInit(): void {
    this.loadRides();
  }

  private loadRides(): void {
    const rides = this.rideHistoryService.getPassengerRideHistory();
    this.allRides.set(rides);
    this.filteredRides.set(rides);
  }

  onFilterChanged(filter: { startDate: string; endDate: string }): void {
    const start = filter.startDate ? new Date(filter.startDate) : null;
    const end = filter.endDate ? new Date(filter.endDate) : null;
    let filtered = this.rideHistoryService.filterRidesByDateRange(
      this.allRides(),
      start,
      end,
    );
    
    // Apply current sort if active
    if (this.currentSort().field) {
      filtered = this.rideHistoryService.sortRides(filtered, this.currentSort().field, this.currentSort().order);
    }
    
    this.filteredRides.set(filtered);
  }

  onFilterCleared(): void {
    this.startDate.set('');
    this.endDate.set('');
    let rides = this.allRides();
    
    // Apply current sort if active
    if (this.currentSort().field) {
      rides = this.rideHistoryService.sortRides(rides, this.currentSort().field, this.currentSort().order);
    }
    
    this.filteredRides.set(rides);
  }

  onSortChanged(sortState: SortState): void {
    this.currentSort.set(sortState);
    if (sortState.field) {
      const sorted = this.rideHistoryService.sortRides(this.filteredRides(), sortState.field, sortState.order);
      this.filteredRides.set(sorted);
    }
  }

  onRideSelected(ride: Ride): void {
    if (this.selectedRide() === ride) {
      this.selectedRide.set(null);
    } else {
      this.selectedRide.set(ride);
    }
  }

  onRideDetailsClosed(): void {
    this.selectedRide.set(null);
  }

  onFavoriteToggled(ride: Ride): void {
    // TODO: Backend Integration - Implement API call to mark ride as favorite
    // 1. Create or update RidePassenger.isFavorite in the database
    // 2. Call backend endpoint: PATCH /api/rides/{rideId}/favorite with { isFavorite: boolean }
    // 3. Handle optimistic UI update vs. pessimistic (wait for response)
    // 4. Add error handling and user feedback (toast notification)
    // 5. Consider adding isFavorite to the Ride DTO in backend
    
    // For now: local toggle only
    const updatedRide = { ...ride, isFavorite: !ride.isFavorite };
    
    // Update in allRides
    const allRidesIndex = this.allRides().findIndex(r => r.id === ride.id);
    if (allRidesIndex !== -1) {
      const updated = [...this.allRides()];
      updated[allRidesIndex] = updatedRide;
      this.allRides.set(updated);
    }
    
    // Update in filteredRides
    const filteredIndex = this.filteredRides().findIndex(r => r.id === ride.id);
    if (filteredIndex !== -1) {
      const updated = [...this.filteredRides()];
      updated[filteredIndex] = updatedRide;
      this.filteredRides.set(updated);
    }
    
    // Update selectedRide if it's the same ride
    if (this.selectedRide()?.id === ride.id) {
      this.selectedRide.set(updatedRide);
    }
  }
}
