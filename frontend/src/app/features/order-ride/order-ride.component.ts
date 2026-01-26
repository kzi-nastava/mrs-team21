import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MapComponent } from '../map/map.component';

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

  ngOnInit(): void {
    const order = this.rideOrder();
    if (order.pickup && order.destination) {
      this.calculatePrice();
    }
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
    // Hardcoded placeholder calculation
    // Formula: base price per vehicle type + km * 120
    const basePrices: Record<string, number> = {
      standard: 150,
      luxury: 300,
      van: 250,
    };

    // Simulate distance calculation (in real app, use Google Maps API)
    const simulatedDistance = Math.floor(Math.random() * 20) + 2;
    const simulatedDuration = Math.floor((simulatedDistance / 40) * 60);

    const order = this.rideOrder();
    const basePrice = basePrices[order.vehicleType];
    const distancePrice = simulatedDistance * 120;
    const totalPrice = basePrice + distancePrice;

    this.rideOrder.set({
      ...order,
      estimatedPrice: totalPrice,
      estimatedDistance: simulatedDistance,
      estimatedDuration: simulatedDuration,
    });
  }

  confirmRide(): void {
    // TODO: Submit ride order to backend
    console.log('Ride order confirmed:', this.rideOrder());
    alert('Ride requested! (Feature to be integrated with backend)');
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
