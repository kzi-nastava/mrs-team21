import { Component, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { RideHistoryService } from '../../services/ride-history.service';
import { Ride } from '../../models';
import { adminHistoryConfig } from '../../config/admin-history.config';
import { RideHistoryTableComponent, SortState } from '../../components/ride-history-table/ride-history-table.component';
import { RideHistoryFiltersComponent } from '../../components/ride-history-filters/ride-history-filters.component';
import { RideDetailsComponent } from '../../components/ride-details/ride-details.component';
import { ToastService } from '../../../../shared/services/toast.service';

/**
 * Smart page component for admin ride history.
 * Handles data fetching and state management for admin role.
 * Uses shared presentational components configured for admin view with full visibility.
 */
@Component({
  selector: 'app-admin-history-page',
  standalone: true,
  imports: [
    CommonModule,
    RideHistoryTableComponent,
    RideHistoryFiltersComponent,
    RideDetailsComponent,
  ],
  templateUrl: './admin-history-page.component.html',
  styleUrl: './admin-history-page.component.scss',
})
export class AdminHistoryPageComponent implements OnInit {
  private readonly rideHistoryService = inject(RideHistoryService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  allRides = signal<Ride[]>([]);
  filteredRides = signal<Ride[]>([]);
  startDate = signal<string>('');
  endDate = signal<string>('');
  selectedRide = signal<Ride | null>(null);
  currentSort = signal<SortState>({ field: null, order: 'asc' });

  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = signal<number>(10);
  isLoading = signal<boolean>(false);

  config = adminHistoryConfig;

  ngOnInit(): void {
    this.loadRides();
  }

  private loadRides(): void {
    this.isLoading.set(true);
    const fromDate = this.startDate() ? new Date(this.startDate()) : null;
    const toDate = this.endDate() ? new Date(this.endDate()) : null;

    this.rideHistoryService
      .getAllAdminRideHistory(fromDate, toDate)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (rides) => {
          this.allRides.set(rides);
          this.applySortingAndPagination();
        },
        error: (error) => {
          console.error('Failed to load admin ride history', error);
          this.toastService.error('Failed to load ride history. Please try again.');
          this.allRides.set([]);
          this.filteredRides.set([]);
          this.totalPages.set(0);
          this.totalElements.set(0);
        },
      });
  }

  private applySortingAndPagination(): void {
    let rides = [...this.allRides()];

    if (this.currentSort().field) {
      rides = this.rideHistoryService.sortRides(
        rides,
        this.currentSort().field,
        this.currentSort().order,
      );
    }

    const totalElements = rides.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / this.pageSize()));
    const currentPage = Math.min(this.currentPage(), totalPages - 1);
    const start = currentPage * this.pageSize();
    const end = start + this.pageSize();

    this.totalElements.set(totalElements);
    this.totalPages.set(totalPages);
    this.currentPage.set(currentPage);
    this.filteredRides.set(rides.slice(start, end));
  }

  onFilterChanged(filter: { startDate: string; endDate: string }): void {
    this.startDate.set(filter.startDate);
    this.endDate.set(filter.endDate);
    this.currentPage.set(0);
    this.loadRides();
  }

  onFilterCleared(): void {
    this.startDate.set('');
    this.endDate.set('');
    this.currentPage.set(0);
    this.loadRides();
  }

  onSortChanged(sortState: SortState): void {
    this.currentSort.set(sortState);
    this.currentPage.set(0);
    this.applySortingAndPagination();
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

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages()) {
      return;
    }
    this.currentPage.set(page);
    this.applySortingAndPagination();
  }

  goToPreviousPage(): void {
    if (this.currentPage() > 0) {
      this.onPageChange(this.currentPage() - 1);
    }
  }

  goToNextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.onPageChange(this.currentPage() + 1);
    }
  }
}
