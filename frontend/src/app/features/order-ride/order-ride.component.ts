import { Component, OnInit } from '@angular/core';
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
  currentStep: 1 | 2 | 3 = 1;

  rideOrder: RideOrder = {
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
  };

  vehicleTypes = [
    { id: 'standard', name: 'Standard', capacity: 3 },
    { id: 'luxury', name: 'Luxury', capacity: 3 },
    { id: 'van', name: 'Van', capacity: 6 },
  ];

  minDate = '';
  maxDate = '';
  nextStopId = 0;
  nextPassengerId = 0;
  termsAccepted = false;
  scheduleHours: number | null = null;
  scheduleMinutes: number | null = null;

  ngOnInit(): void {
    this.calculatePrice();
  }

  goToStep(step: 1 | 2 | 3): void {
    if (this.isStepValid(this.currentStep) || step < this.currentStep) {
      this.currentStep = step;
    }
  }

  nextStep(): void {
    if (this.currentStep < 3 && this.isStepValid(this.currentStep)) {
      this.currentStep = (this.currentStep + 1) as 1 | 2 | 3;
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep = (this.currentStep - 1) as 1 | 2 | 3;
    }
  }

  isStepValid(step: 1 | 2 | 3): boolean {
    switch (step) {
      case 1:
        // All addresses must be filled and stops must not have empty addresses
        const hasValidAddresses =
          this.rideOrder.pickup.trim() !== '' && this.rideOrder.destination.trim() !== '';
        const allStopsValid = this.rideOrder.stops.every((stop) => stop.address.trim() !== '');
        return hasValidAddresses && allStopsValid;
      case 2:
        // If scheduling for later, hours or minutes must be set and not exceed 5 hours
        if (!this.rideOrder.scheduleNow) {
          const hours = this.scheduleHours ?? 0;
          const minutes = this.scheduleMinutes ?? 0;
          const totalMinutes = hours * 60 + minutes;

          // Must have at least some time set and not exceed 5 hours (300 minutes)
          return totalMinutes > 0 && totalMinutes <= 300;
        }
        return true;
      case 3:
        // Terms must be accepted
        return this.termsAccepted;
      default:
        return false;
    }
  }

  addStop(): void {
    this.rideOrder.stops.push({
      id: `stop-${this.nextStopId++}`,
      address: '',
    });
  }

  removeStop(id: string): void {
    this.rideOrder.stops = this.rideOrder.stops.filter((s) => s.id !== id);
  }

  addPassenger(): void {
    const vehicleCapacity = this.vehicleTypes.find((v) => v.id === this.rideOrder.vehicleType)?.capacity || 3;
    if (this.rideOrder.passengers.length < vehicleCapacity) {
      this.rideOrder.passengers.push({
        id: `passenger-${this.nextPassengerId++}`,
        email: '',
      });
    }
  }

  removePassenger(id: string): void {
    this.rideOrder.passengers = this.rideOrder.passengers.filter((p) => p.id !== id);
  }

  canAddPassenger(): boolean {
    const vehicleCapacity = this.vehicleTypes.find((v) => v.id === this.rideOrder.vehicleType)?.capacity || 3;
    return this.rideOrder.passengers.length < vehicleCapacity;
  }

  getVehicleCapacity(): number {
    return this.vehicleTypes.find((v) => v.id === this.rideOrder.vehicleType)?.capacity || 3;
  }

  onRouteChange(): void {
    this.calculatePrice();
  }

  setScheduleMode(mode: 'now' | 'later'): void {
    this.rideOrder.scheduleNow = mode === 'now';
    if (!this.rideOrder.scheduleNow) {
      this.scheduleHours = null;
      this.scheduleMinutes = null;
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

    const basePrice = basePrices[this.rideOrder.vehicleType];
    const distancePrice = simulatedDistance * 120;
    const totalPrice = basePrice + distancePrice;

    this.rideOrder.estimatedPrice = totalPrice;
    this.rideOrder.estimatedDistance = simulatedDistance;
    this.rideOrder.estimatedDuration = simulatedDuration;
  }

  confirmRide(): void {
    // TODO: Submit ride order to backend
    console.log('Ride order confirmed:', this.rideOrder);
    alert('Ride requested! (Feature to be integrated with backend)');
  }

  getVehicleTypeName(): string {
    const vehicle = this.vehicleTypes.find((v) => v.id === this.rideOrder.vehicleType);
    return vehicle?.name || 'Unknown';
  }

  getStepStatus(step: 1 | 2 | 3): 'active' | 'completed' | 'pending' {
    if (step === this.currentStep) return 'active';
    if (step < this.currentStep) return 'completed';
    return 'pending';
  }
}
