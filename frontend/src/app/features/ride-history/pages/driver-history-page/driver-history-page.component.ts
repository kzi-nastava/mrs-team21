import { Component, OnInit, signal, computed, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { RideHistoryService } from '../../services/ride-history.service';
import { Ride } from '../../models';
import { driverHistoryConfig } from '../../config/driver-history.config';
import { RideHistoryTableComponent, SortState } from '../../components/ride-history-table/ride-history-table.component';
import { RideHistoryFiltersComponent } from '../../components/ride-history-filters/ride-history-filters.component';
import { RideDetailsComponent } from '../../components/ride-details/ride-details.component';

const driverUpcomingConfig = {
  tableColumns: [
    { key: 'dateTime', label: 'Scheduled Time', visible: true },
    { key: 'route', label: 'Route', visible: true },
    { key: 'status', label: 'Status', visible: true },
  ],
  detailsConfig: {
    showPassengers: false,
    showDriverInfo: false,
    showPanicAlert: false,
    showCancellationInfo: true,
    showEarnings: false,
    showRatings: false,
    showRatingAction: false,
  },
  pageTitle: 'Upcoming Scheduled Rides',
  pageSubtitle: 'View your scheduled pickups and prepare for the next ride',
  showEarningsInTable: false,
};

/**
 * Smart page component for driver ride history.
 * Handles data fetching and state management for driver role.
 * Uses shared presentational components configured for driver view.
 */
@Component({
  selector: 'app-driver-history-page',
  standalone: true,
  imports: [
    CommonModule,
    RideHistoryTableComponent,
    RideHistoryFiltersComponent,
    RideDetailsComponent,
  ],
  templateUrl: './driver-history-page.component.html',
  styleUrl: './driver-history-page.component.scss',
})
export class DriverHistoryPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  allRides = signal<Ride[]>([]);
  filteredRides = signal<Ride[]>([]);
  startDate = signal<string>('');
  endDate = signal<string>('');
  selectedRide = signal<Ride | null>(null);
  currentSort = signal<SortState>({ field: null, order: 'asc' });
  viewMode = signal<'history' | 'upcoming'>('history');
  isUpcomingLoading = signal<boolean>(false);

  config = computed(() =>
    this.viewMode() === 'upcoming' ? driverUpcomingConfig : driverHistoryConfig,
  );

  constructor(private rideHistoryService: RideHistoryService) {}

  ngOnInit(): void {
    this.setDefaultDateRange();
    this.loadRides();
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const view = params.get('view');
        if (view === 'upcoming') {
          this.setViewMode('upcoming');
        } else {
          this.setViewMode('history');
        }
      });
  }

  private loadRides(): void {
    const rides = this.rideHistoryService.getDriverRideHistory();
    this.allRides.set(rides);
    this.filteredRides.set(rides);
  }

  private loadUpcomingRides(): void {
    const driverId = 1;
    this.isUpcomingLoading.set(true);
    this.rideHistoryService
      .getUpcomingDriverRides(driverId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isUpcomingLoading.set(false)),
      )
      .subscribe({
        next: (rides) => {
          this.allRides.set(rides);
          this.filteredRides.set(rides);
        },
        error: (error) => {
          console.error('Failed to load upcoming rides', error);
          this.allRides.set([]);
          this.filteredRides.set([]);
        },
      });
  }

  private setDefaultDateRange(): void {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);

    this.endDate.set(endDate.toISOString().split('T')[0]);
    this.startDate.set(startDate.toISOString().split('T')[0]);
  }

  onFilterChanged(filter: { startDate: string; endDate: string }): void {
    if (this.viewMode() === 'upcoming') {
      return;
    }
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
    if (this.viewMode() === 'upcoming') {
      return;
    }
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

  setViewMode(mode: 'history' | 'upcoming'): void {
    if (this.viewMode() === mode) {
      return;
    }
    this.viewMode.set(mode);
    this.selectedRide.set(null);
    if (mode === 'upcoming') {
      this.loadUpcomingRides();
    } else {
      this.loadRides();
    }
  }
}
