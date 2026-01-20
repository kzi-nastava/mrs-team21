export interface Passenger {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
}

export interface RideHistory {
  id: string;
  startTime: Date;
  endTime: Date | null;
  origin: string;
  destination: string;
  cost: number;
  isCancelled: boolean;
  cancelledBy: 'DRIVER' | 'PASSENGER' | null;
  cancellationReason?: string;
  panicActivated: boolean;
  passengers: Passenger[];
  vehicleType: 'STANDARD' | 'LUXURY' | 'VAN';
}

