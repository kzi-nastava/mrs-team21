import { Component, Input, Output, EventEmitter, signal, inject, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';
import { EstimateService, AddressSuggestion } from '../../services/estimate-ride.service';
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
export class RideEstimatePanelComponent implements OnInit {
  @Input() isOpen = false;
  @Output() panelClosed = new EventEmitter<void>();
  /** Emits route coordinates [lng, lat][] when estimate is calculated, for drawing on map. */
  @Output() routeReady = new EventEmitter<[number, number][] | undefined>();

  pickupLocation = signal('');
  destinationLocation = signal('');
  /** Not used in estimation UI; only registered users can add stops when ordering. */
  stops = signal<string[]>([]);
  vehicleType = signal<VehicleTypeName>(VehicleTypeName.STANDARD);
  showResults = signal(false);
  isLoading = signal(false);

  pickupSuggestions = signal<AddressSuggestion[]>([]);
  destinationSuggestions = signal<AddressSuggestion[]>([]);
  showPickupSuggestions = signal(false);
  showDestinationSuggestions = signal(false);

  estimate = signal<RideEstimate | null>(null);

  private estimateService = inject(EstimateService);
  private destroyRef = inject(DestroyRef);
  private pickupQuery$ = new Subject<string>();
  private destinationQuery$ = new Subject<string>();

  vehicleTypes = Object.values(VehicleTypeName);

  ngOnInit(): void {
    this.pickupQuery$
      .pipe(
        debounceTime(300),
        switchMap((q) => this.estimateService.getAddressSuggestions(q)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list) => {
        this.pickupSuggestions.set(list);
        this.showPickupSuggestions.set(list.length > 0);
      });
    this.destinationQuery$
      .pipe(
        debounceTime(300),
        switchMap((q) => this.estimateService.getAddressSuggestions(q)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list) => {
        this.destinationSuggestions.set(list);
        this.showDestinationSuggestions.set(list.length > 0);
      });
  }

  closePanel(): void {
    this.routeReady.emit(undefined);
    this.panelClosed.emit();
  }

  calculateEstimate(): void {
    if (!this.pickupLocation() || !this.destinationLocation()) return;

    this.isLoading.set(true);
    this.estimateService
      .getEstimate(
        this.pickupLocation(),
        this.destinationLocation(),
        this.vehicleType(),
        [],
      )
      .subscribe({
        next: (response) => {
          this.estimate.set({
            time: response.durationInMinutes,
            price: response.estimatedPrice,
            distance: response.distanceInKm,
          });
          this.showResults.set(true);
          this.isLoading.set(false);
          this.routeReady.emit(response.routeCoordinates ?? undefined);
        },
        error: (err) => {
          console.error('Estimate error:', err);
          this.isLoading.set(false);
          this.routeReady.emit(undefined);
        },
      });
  }

  updatePickup(value: string): void {
    this.pickupLocation.set(value);
    this.pickupQuery$.next(value);
  }

  updateDestination(value: string): void {
    this.destinationLocation.set(value);
    this.destinationQuery$.next(value);
  }

  selectPickupSuggestion(s: AddressSuggestion): void {
    this.pickupLocation.set(s.address);
    this.pickupSuggestions.set([]);
    this.showPickupSuggestions.set(false);
  }

  selectDestinationSuggestion(s: AddressSuggestion): void {
    this.destinationLocation.set(s.address);
    this.destinationSuggestions.set([]);
    this.showDestinationSuggestions.set(false);
  }

  hidePickupSuggestions(): void {
    setTimeout(() => this.showPickupSuggestions.set(false), 150);
  }

  hideDestinationSuggestions(): void {
    setTimeout(() => this.showDestinationSuggestions.set(false), 150);
  }
}
