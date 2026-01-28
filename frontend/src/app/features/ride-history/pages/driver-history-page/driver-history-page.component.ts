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
import { AuthService } from '../../../../shared/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';

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
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly rideHistoryService = inject(RideHistoryService);

  // Ride data
  filteredRides = signal<Ride[]>([]);
  selectedRide = signal<Ride | null>(null);

  // Filter state
  startDate = signal<string>('');
  endDate = signal<string>('');
  currentSort = signal<SortState>({ field: null, order: 'asc' });

  // Pagination state
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = signal<number>(10);

  // Loading state
  isLoading = signal<boolean>(false);
  isUpcomingLoading = signal<boolean>(false);

  // View mode
  viewMode = signal<'history' | 'upcoming'>('history');

  config = computed(() =>
    this.viewMode() === 'upcoming' ? driverUpcomingConfig : driverHistoryConfig,
  );

  ngOnInit(): void {
    this.setDefaultDateRange();
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const view = params.get('view');
        if (view === 'upcoming') {
          this.setViewMode('upcoming', true);
        } else {
          this.setViewMode('history', true);
        }
      });
  }

  private loadRides(): void {
    const driverId = this.authService.getUserId();
    if (!driverId) {
      this.toastService.error('Please log in to view your ride history');
      return;
    }

    this.isLoading.set(true);
    const fromDate = this.startDate() ? new Date(this.startDate()) : null;
    const toDate = this.endDate() ? new Date(this.endDate()) : null;

    this.rideHistoryService
      .getDriverRideHistory(
        driverId,
        fromDate,
        toDate,
        this.currentPage(),
        this.pageSize(),
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (response) => {
          let rides = response.rides;
          // Apply client-side sorting if active
          if (this.currentSort().field) {
            rides = this.rideHistoryService.sortRides(
              rides,
              this.currentSort().field,
              this.currentSort().order,
            );
          }
          this.filteredRides.set(rides);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
          this.currentPage.set(response.currentPage);
        },
        error: (error) => {
          console.error('Failed to load ride history', error);
          this.toastService.error('Failed to load ride history. Please try again.');
          this.filteredRides.set([]);
          this.totalPages.set(0);
          this.totalElements.set(0);
        },
      });
  }

  private loadUpcomingRides(): void {
    const driverId = this.authService.getUserId();
    if (!driverId) {
      this.toastService.error('Please log in to view your upcoming rides');
      return;
    }

    this.isUpcomingLoading.set(true);
    this.rideHistoryService
      .getUpcomingDriverRides(driverId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isUpcomingLoading.set(false)),
      )
      .subscribe({
        next: (rides) => {
          this.filteredRides.set(rides);
          // Reset pagination for upcoming rides (no pagination)
          this.totalPages.set(1);
          this.totalElements.set(rides.length);
          this.currentPage.set(0);
        },
        error: (error) => {
          console.error('Failed to load upcoming rides', error);
          this.toastService.error('Failed to load upcoming rides. Please try again.');
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
    this.startDate.set(filter.startDate);
    this.endDate.set(filter.endDate);
    // Reset to first page when filter changes
    this.currentPage.set(0);
    // Reload with new filter (server-side filtering)
    this.loadRides();
  }

  onFilterCleared(): void {
    if (this.viewMode() === 'upcoming') {
      return;
    }
    this.startDate.set('');
    this.endDate.set('');
    // Reset to first page
    this.currentPage.set(0);
    // Reload without filter
    this.loadRides();
  }

  onSortChanged(sortState: SortState): void {
    this.currentSort.set(sortState);
    if (sortState.field) {
      // Apply client-side sorting on current page
      const sorted = this.rideHistoryService.sortRides(
        this.filteredRides(),
        sortState.field,
        sortState.order,
      );
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

  setViewMode(mode: 'history' | 'upcoming', forceLoad = false): void {
    if (this.viewMode() === mode && !forceLoad) {
      return;
    }
    this.viewMode.set(mode);
    this.selectedRide.set(null);
    this.currentPage.set(0);
    if (mode === 'upcoming') {
      this.loadUpcomingRides();
    } else {
      this.loadRides();
    }
  }

  /**
   * Handle page change from pagination controls.
   */
  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages()) {
      return;
    }
    this.currentPage.set(page);
    this.loadRides();
  }

  /**
   * Go to the previous page.
   */
  goToPreviousPage(): void {
    if (this.currentPage() > 0) {
      this.onPageChange(this.currentPage() - 1);
    }
  }

  /**
   * Go to the next page.
   */
  goToNextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.onPageChange(this.currentPage() + 1);
    }
  }
}
