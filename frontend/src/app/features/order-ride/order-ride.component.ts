import { Component, OnInit, signal, computed, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, map, switchMap } from 'rxjs/operators';
import { MapComponent } from '../map/map.component';
import { EstimateService, AddressSuggestion } from '../landing/services/estimate-ride.service';
import { AuthService } from '../../shared/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { VehicleTypeName } from '../landing/models/estimate.model';
import { LocationDTO } from '../landing/models/estimate.model';

export interface Stop {
  id: string;
  address: string;
}

export interface Passenger {
  id: string;
  email: string;
}

export interface RideOrder {
  pickup: string;
  destination: string;
  stops: Stop[];
  passengers: Passenger[];
  vehicleType: 'standard' | 'luxury' | 'van';
  scheduleNow: boolean;
  scheduledTime?: string;
  scheduledFor?: string; // ISO string for API
  specialRequests: string;
  babySeat: boolean;
  petTransport: boolean;
  estimatedPrice: number;
  estimatedDistance: number;
  estimatedDuration: number;
}

@Component({
  selector: 'app-order-ride',
  standalone: true,
  imports: [CommonModule, FormsModule, MapComponent],
  templateUrl: './order-ride.component.html',
  styleUrls: ['./order-ride.component.scss'],
})
export class OrderRideComponent implements OnInit {
  // State signals
  currentStep = signal<1 | 2 | 3>(1);
  rideOrder = signal<RideOrder>({
    pickup: '',
    destination: '',
    stops: [],
    passengers: [],
    vehicleType: 'standard',
    scheduleNow: true,
    specialRequests: '',
    babySeat: false,
    petTransport: false,
    estimatedPrice: 0,
    estimatedDistance: 0,
    estimatedDuration: 0,
  });

  vehicleTypes = [
    { id: 'standard', name: 'Standard', capacity: 3 },
    { id: 'luxury', name: 'Luxury', capacity: 3 },
    { id: 'van', name: 'Van', capacity: 6 },
  ];

  minDate = signal<string>('');
  maxDate = signal<string>('');
  nextStopId = signal<number>(0);
  nextPassengerId = signal<number>(0);
  termsAccepted = signal<boolean>(false);
  scheduleHours = signal<number | null>(null);
  scheduleMinutes = signal<number | null>(null);
  routeCoordinates = signal<[number, number][] | undefined>(undefined);
  showRoute = signal(false);
  estimateLoading = signal(false);
  estimateError = signal<string | null>(null);
  /** Geocoded waypoints [start, ...stops, destination] from last successful estimate, for confirm. */
  lastGeocodedWaypoints = signal<LocationDTO[] | null>(null);

  /** API address suggestions; showSuggestionsFor indicates which field (pickup, destination, or stop id). */
  addressSuggestions = signal<AddressSuggestion[]>([]);
  showSuggestionsFor = signal<'pickup' | 'destination' | string | null>(null);

  private estimateService = inject(EstimateService);
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private apiUrl = environment.apiBaseUrl;
  private pickupQuery$ = new Subject<string>();
  private destinationQuery$ = new Subject<string>();
  private stopQuery$ = new Subject<{ stopId: string; query: string }>();

  // Computed signals
  vehicleCapacity = computed(() => {
    const capacity = this.vehicleTypes.find(
      (v) => v.id === this.rideOrder().vehicleType
    )?.capacity;
    return capacity || 3;
  });

  canAddPassenger = computed(() => {
    return this.rideOrder().passengers.length < this.vehicleCapacity();
  });

  // Helper methods for template binding with signals
  updatePickup(value: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({ ...order, pickup: value });
  }

  updateDestination(value: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({ ...order, destination: value });
  }

  updateVehicleType(value: 'standard' | 'luxury' | 'van'): void {
    const order = this.rideOrder();
    this.rideOrder.set({ ...order, vehicleType: value });
  }

  updateSpecialRequests(value: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({ ...order, specialRequests: value });
  }

  updateBabySeat(value: boolean): void {
    const order = this.rideOrder();
    this.rideOrder.set({ ...order, babySeat: value });
  }

  updatePetTransport(value: boolean): void {
    const order = this.rideOrder();
    this.rideOrder.set({ ...order, petTransport: value });
  }

  updateStopAddress(id: string, address: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({
      ...order,
      stops: order.stops.map((s) => (s.id === id ? { ...s, address } : s)),
    });
  }

  updatePassengerEmail(id: string, email: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({
      ...order,
      passengers: order.passengers.map((p) => (p.id === id ? { ...p, email } : p)),
    });
  }

  private toVehicleTypeName(v: 'standard' | 'luxury' | 'van'): VehicleTypeName {
    return VehicleTypeName[v.toUpperCase() as keyof typeof VehicleTypeName] ?? VehicleTypeName.STANDARD;
  }

  ngOnInit(): void {
    const order = this.rideOrder();
    if (order.pickup && order.destination) {
      this.calculatePrice();
    }
    this.pickupQuery$
      .pipe(
        debounceTime(300),
        switchMap((q) => this.estimateService.getAddressSuggestions(q)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list) => {
        this.addressSuggestions.set(list);
        this.showSuggestionsFor.set(list.length > 0 ? 'pickup' : null);
      });
    this.destinationQuery$
      .pipe(
        debounceTime(300),
        switchMap((q) => this.estimateService.getAddressSuggestions(q)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list) => {
        this.addressSuggestions.set(list);
        this.showSuggestionsFor.set(list.length > 0 ? 'destination' : null);
      });
    this.stopQuery$
      .pipe(
        debounceTime(300),
        switchMap(({ stopId, query }) =>
          this.estimateService.getAddressSuggestions(query).pipe(
            map((list) => ({ stopId, list })),
          )
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ stopId, list }) => {
        this.addressSuggestions.set(list);
        this.showSuggestionsFor.set(list.length > 0 ? stopId : null);
      });
  }

  onPickupInput(value: string): void {
    this.pickupQuery$.next(value);
  }

  onDestinationInput(value: string): void {
    this.destinationQuery$.next(value);
  }

  onStopAddressInput(stopId: string, value: string): void {
    this.stopQuery$.next({ stopId, query: value });
  }

  selectAddressSuggestion(address: string): void {
    const forField = this.showSuggestionsFor();
    if (forField === 'pickup') this.updatePickup(address);
    else if (forField === 'destination') this.updateDestination(address);
    else if (typeof forField === 'string') this.updateStopAddress(forField, address);
    this.addressSuggestions.set([]);
    this.showSuggestionsFor.set(null);
  }

  hideAddressSuggestions(): void {
    setTimeout(() => this.showSuggestionsFor.set(null), 150);
  }

  goToStep(step: 1 | 2 | 3): void {
    if (this.isStepValid(this.currentStep()) || step < this.currentStep()) {
      this.currentStep.set(step);
    }
  }

  nextStep(): void {
    if (this.currentStep() < 3 && this.isStepValid(this.currentStep())) {
      // Build scheduled time string if scheduling for later
      if (this.currentStep() === 2 && !this.rideOrder().scheduleNow) {
        const hours = this.scheduleHours() ?? 0;
        const minutes = this.scheduleMinutes() ?? 0;
        const order = this.rideOrder();
        this.rideOrder.set({
          ...order,
          scheduledTime: `${hours}h ${minutes}m from now`,
        });
      }
      this.currentStep.set((this.currentStep() + 1) as 1 | 2 | 3);
    }
  }

  previousStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.set((this.currentStep() - 1) as 1 | 2 | 3);
    }
  }

  isStepValid(step: 1 | 2 | 3): boolean {
    const order = this.rideOrder();
    switch (step) {
      case 1:
        // All addresses must be filled and stops must not have empty addresses
        const hasValidAddresses =
          order.pickup.trim() !== '' && order.destination.trim() !== '';
        const allStopsValid = order.stops.every((stop) => stop.address.trim() !== '');
        return hasValidAddresses && allStopsValid;
      case 2:
        // If scheduling for later, hours or minutes must be set and not exceed 5 hours
        if (!order.scheduleNow) {
          const hours = this.scheduleHours() ?? 0;
          const minutes = this.scheduleMinutes() ?? 0;
          const totalMinutes = hours * 60 + minutes;

          // Must have at least some time set and not exceed 5 hours (300 minutes)
          return totalMinutes > 0 && totalMinutes <= 300;
        }
        return true;
      case 3:
        // Terms must be accepted
        return this.termsAccepted();
      default:
        return false;
    }
  }

  addStop(): void {
    const order = this.rideOrder();
    const stopId = this.nextStopId();
    this.rideOrder.set({
      ...order,
      stops: [
        ...order.stops,
        {
          id: `stop-${stopId}`,
          address: '',
        },
      ],
    });
    this.nextStopId.set(stopId + 1);
  }

  removeStop(id: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({
      ...order,
      stops: order.stops.filter((s) => s.id !== id),
    });
  }

  addPassenger(): void {
    if (!this.canAddPassenger()) return;
    
    const order = this.rideOrder();
    const passengerId = this.nextPassengerId();
    this.rideOrder.set({
      ...order,
      passengers: [
        ...order.passengers,
        {
          id: `passenger-${passengerId}`,
          email: '',
        },
      ],
    });
    this.nextPassengerId.set(passengerId + 1);
  }

  removePassenger(id: string): void {
    const order = this.rideOrder();
    this.rideOrder.set({
      ...order,
      passengers: order.passengers.filter((p) => p.id !== id),
    });
  }

  getVehicleCapacity(): number {
    return this.vehicleCapacity();
  }

  onRouteChange(): void {
    this.calculatePrice();
  }

  setScheduleMode(mode: 'now' | 'later'): void {
    const order = this.rideOrder();
    this.rideOrder.set({
      ...order,
      scheduleNow: mode === 'now',
    });
    if (mode === 'later') {
      this.scheduleHours.set(null);
      this.scheduleMinutes.set(null);
    }
  }

  calculatePrice(): void {
    const order = this.rideOrder();
    if (!order.pickup?.trim() || !order.destination?.trim()) return;
    const stopAddresses = order.stops.map((s) => s.address.trim()).filter(Boolean);
    this.estimateLoading.set(true);
    this.estimateError.set(null);
    this.estimateService
      .getEstimateWithWaypoints(
        order.pickup,
        order.destination,
        this.toVehicleTypeName(order.vehicleType),
        stopAddresses,
      )
      .subscribe({
        next: (result) => {
          this.rideOrder.set({
            ...order,
            estimatedPrice: result.estimatedPrice,
            estimatedDistance: result.distanceInKm,
            estimatedDuration: result.durationInMinutes,
          });
          this.routeCoordinates.set(result.routeCoordinates ?? undefined);
          this.showRoute.set(Array.isArray(result.routeCoordinates) && result.routeCoordinates.length >= 2);
          this.lastGeocodedWaypoints.set(result.geocodedWaypoints ?? null);
          this.estimateLoading.set(false);
        },
        error: (err) => {
          this.estimateError.set(err?.message ?? 'Failed to get estimate');
          this.estimateLoading.set(false);
        },
      });
  }

  confirmRide(): void {
    const order = this.rideOrder();
    const waypoints = this.lastGeocodedWaypoints();
    if (!waypoints || waypoints.length < 2) {
      this.estimateError.set('Please calculate estimate first (enter addresses and wait for result).');
      return;
    }
    const userId = this.auth.getUserId();
    if (userId == null) {
      this.estimateError.set('You must be logged in to order a ride.');
      return;
    }
    const hours = this.scheduleHours() ?? 0;
    const minutes = this.scheduleMinutes() ?? 0;
    const scheduledFor =
      order.scheduleNow
        ? null
        : new Date(Date.now() + hours * 3600000 + minutes * 60000).toISOString();
    const body = {
      waypoints: waypoints.map((wp, i) => ({
        address: wp.address ?? '',
        lat: wp.latitude,
        lng: wp.longitude,
        order: i + 1,
      })),
      vehicleType: order.vehicleType.toUpperCase(),
      babyTransport: order.babySeat,
      petTransport: order.petTransport,
      linkedPassengerEmails: order.passengers.map((p) => p.email).filter(Boolean),
      scheduledFor,
    };
    this.http
      .post<{ id: number }>(`${this.apiUrl}/rides?orderingPassengerId=${userId}`, body)
      .subscribe({
        next: (ride) => {
          this.router.navigate(['/ride-tracking', ride.id]);
        },
        error: (err) => {
          this.estimateError.set(err?.error?.message ?? err?.message ?? 'Failed to create ride');
        },
      });
  }

  getVehicleTypeName(): string {
    const vehicle = this.vehicleTypes.find(
      (v) => v.id === this.rideOrder().vehicleType
    );
    return vehicle?.name || 'Unknown';
  }

  getStepStatus(step: 1 | 2 | 3): 'active' | 'completed' | 'pending' {
    if (step === this.currentStep()) return 'active';
    if (step < this.currentStep()) return 'completed';
    return 'pending';
  }
}
