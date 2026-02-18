import { Component, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { RideHistoryService } from '../../services/ride-history.service';
import { Ride } from '../../models';
import { passengerHistoryConfig } from '../../config/passenger-history.config';
import { RideHistoryTableComponent, SortState } from '../../components/ride-history-table/ride-history-table.component';
import { RideHistoryFiltersComponent } from '../../components/ride-history-filters/ride-history-filters.component';
import { RideDetailsComponent } from '../../components/ride-details/ride-details.component';
import {
  RatingModalComponent,
  RideForRating,
} from '../../components/rating-modal/rating-modal.component';
import { ReviewResponse, ReviewService } from '../../services/review.service';
import { AuthService } from '../../../../shared/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';

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
    RatingModalComponent,
  ],
  templateUrl: './passenger-history-page.component.html',
  styleUrl: './passenger-history-page.component.scss',
})
export class PassengerHistoryPageComponent implements OnInit {
  private readonly rideHistoryService = inject(RideHistoryService);
  private readonly reviewService = inject(ReviewService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);

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

  // Rating modal state
  showRatingModal = signal<boolean>(false);
  rideToRate = signal<RideForRating | null>(null);

  config = passengerHistoryConfig;

  ngOnInit(): void {
    this.loadRides();
  }

  private loadRides(): void {
    const passengerId = this.authService.getUserId();
    if (!passengerId) {
      this.toastService.error('Please log in to view your ride history');
      return;
    }

    this.isLoading.set(true);
    const fromDate = this.startDate() ? new Date(this.startDate()) : null;
    const toDate = this.endDate() ? new Date(this.endDate()) : null;

    this.rideHistoryService
      .getAllPassengerRideHistory(
        passengerId,
        fromDate,
        toDate,
      )
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
          console.error('Failed to load passenger ride history', error);
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
      // Fetch rating status when ride is selected (for finished rides only)
      if (ride.status === 'FINISHED' || (!ride.isCancelled && ride.endTime)) {
        this.fetchRatingStatusForRide(ride.id);
      }
    }
  }

  /**
   * Fetch rating status from backend when viewing a ride.
   * This populates canRate, hasReview, and daysRemainingToRate.
   */
  private fetchRatingStatusForRide(rideId: string): void {
    this.reviewService.getRatingStatus(rideId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (status) => {
          const updateRide = (ride: Ride): Ride => ({
            ...ride,
            hasReview: status.hasReview,
            canRate: status.canRate,
            daysRemainingToRate: status.daysRemaining,
            ratingDeadline: status.ratingDeadline ? new Date(status.ratingDeadline) : undefined,
          });

          this.filteredRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
          this.allRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
          this.applySortingAndPagination();

          // Update selectedRide
          if (this.selectedRide()?.id === rideId) {
            this.selectedRide.update((r) => (r ? updateRide(r) : r));
          }

          // If already reviewed, fetch review details so we can show actual ratings
          if (status.hasReview) {
            this.fetchRideReviewForRide(rideId);
          }
        },
        error: (error) => {
          // Silently handle - user may not be the ordering passenger
          console.debug('Could not fetch rating status:', error);
        },
      });
  }

  /**
   * Fetch review details for a ride (ratings/comment) so the UI can display them.
   */
  private fetchRideReviewForRide(rideId: string): void {
    this.reviewService.getRideReviews(rideId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reviews) => {
          const review = reviews?.[0];
          if (!review) return;

          const updateRide = (ride: Ride): Ride => ({
            ...ride,
            driverRating: review.ratingDriver,
            vehicleRating: review.ratingVehicle,
            reviewComment: review.comment,
          });

          this.filteredRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
          this.allRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
          this.applySortingAndPagination();
          if (this.selectedRide()?.id === rideId) {
            this.selectedRide.update((r) => (r ? updateRide(r) : r));
          }
        },
        error: (error) => {
          console.debug('Could not fetch ride reviews:', error);
        },
      });
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

    this.allRides.update((rides) => rides.map((r) => (r.id === ride.id ? updatedRide : r)));
    this.applySortingAndPagination();
    
    // Update selectedRide if it's the same ride
    if (this.selectedRide()?.id === ride.id) {
      this.selectedRide.set(updatedRide);
    }
  }

  // Rating methods
  onRateRide(ride: Ride): void {
    const rideForRating: RideForRating = {
      id: ride.id,
      driverName: ride.driver ? `${ride.driver.firstName} ${ride.driver.lastName}` : 'Unknown Driver',
      driverPhotoUrl: ride.driver?.photoUrl,
      vehicleModel: undefined, // Not available in current model
      vehicleLicensePlate: undefined, // Not available in current model
      origin: ride.origin,
      destination: ride.destination,
      completedAt: ride.endTime ?? undefined,
    };
    this.rideToRate.set(rideForRating);
    this.showRatingModal.set(true);
  }

  closeRatingModal(): void {
    this.showRatingModal.set(false);
    this.rideToRate.set(null);
  }

  onRatingSubmitted(review: ReviewResponse): void {
    const rideId = String(review.rideId);

    // Immediately show the ratings the passenger just submitted
    const updateRide = (ride: Ride): Ride => ({
      ...ride,
      driverRating: review.ratingDriver,
      vehicleRating: review.ratingVehicle,
      reviewComment: review.comment,
      hasReview: true,
      canRate: false,
      daysRemainingToRate: 0,
    });

    this.filteredRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
    this.allRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
    this.applySortingAndPagination();
    if (this.selectedRide()?.id === rideId) {
      this.selectedRide.update((r) => (r ? updateRide(r) : r));
    }

    // Sync status from backend (source of truth)
    this.updateRideRatingStatus(rideId);

    this.closeRatingModal();
  }

  private updateRideRatingStatus(rideId: string): void {
    // Fetch updated rating status from backend
    this.reviewService.getRatingStatus(rideId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (status) => {
          const updateRide = (ride: Ride): Ride => ({
            ...ride,
            hasReview: status.hasReview,
            canRate: status.canRate,
            daysRemainingToRate: status.daysRemaining,
          });

          this.filteredRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
          this.allRides.update((rides) => rides.map((r) => (r.id === rideId ? updateRide(r) : r)));
          this.applySortingAndPagination();

          // Update selectedRide if needed
          if (this.selectedRide()?.id === rideId) {
            this.selectedRide.update((r) => (r ? updateRide(r) : r));
          }
        },
        error: (error) => {
          console.error('Failed to fetch rating status:', error);
        },
      });
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
