export interface Ride {
  id: string;
  pickup: string;
  dropoff: string;
  stops?: string[];
  status: RideStatus;
  price?: number;
  distance?: number;
  duration?: number;
  scheduledTime?: Date;
  createdAt: Date;
  startedAt?: Date;
  completedAt?: Date;
  canceledAt?: Date;
  cancelReason?: string;
  canceledBy?: 'passenger' | 'driver' | 'system';
  driver?: RideDriver;
  passenger?: RidePassenger;
  linkedPassengers?: RidePassenger[];
  vehicleType: VehicleType;
  options: RideOptions;
}

export interface RideDriver {
  id: string;
  name: string;
  initials: string;
  phone?: string;
  rating?: number;
  vehicle: {
    model: string;
    plate: string;
    type: VehicleType;
  };
}

export interface RidePassenger {
  id: string;
  name: string;
  initials: string;
  email: string;
  phone?: string;
}

export interface RideOptions {
  babySeats: boolean;
  petFriendly: boolean;
}

export type RideStatus =
  | 'pending'
  | 'searching_driver'
  | 'driver_assigned'
  | 'driver_arriving'
  | 'in_progress'
  | 'completed'
  | 'canceled';

export type VehicleType = 'standard' | 'luxury' | 'van';
