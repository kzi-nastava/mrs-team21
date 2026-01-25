export interface CancelRideResult {
  reasonId: string;
  reasonLabel: string;
  explanation?: string;
}

export interface CancelReason {
  id: string;
  label: string;
  requiresExplanation: boolean;
}

export type RideStatus = 'pending' | 'driver_arriving' | 'in_progress' | 'completed' | 'cancelled';

export interface RideSummaryForCancel {
  rideId: string;
  status: RideStatus;
  startAddress: string;
  destinationAddress: string;
  driver: {
    id: string;
    firstName: string;
    lastName: string;
    rating: number;
    vehicle: string;
  };
  passenger: {
    id: string;
    firstName: string;
    lastName: string;
    rating: number;
    totalRides: number;
  };
}

// Driver cancel reasons
export const DRIVER_CANCEL_REASONS: CancelReason[] = [
  {
    id: 'driver_emergency',
    label: 'Personal emergency',
    requiresExplanation: true,
  },
  {
    id: 'driver_vehicle_issue',
    label: 'Vehicle issue',
    requiresExplanation: true,
  },
  {
    id: 'driver_unsafe_pickup',
    label: 'Unsafe pickup location',
    requiresExplanation: false,
  },
  {
    id: 'driver_passenger_no_show',
    label: 'Passenger not responding',
    requiresExplanation: false,
  },
  {
    id: 'driver_other',
    label: 'Other reason',
    requiresExplanation: true,
  },
];
