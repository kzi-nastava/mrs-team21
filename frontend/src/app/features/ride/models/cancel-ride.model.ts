export interface CancelReason {
  id: string;
  label: string;
  requiresExplanation: boolean;
}

export interface CancelRideResult {
  reasonId: string;
  reasonLabel: string;
  explanation?: string;
}

export interface RideSummaryForCancel {
  id: string;
  pickup: string;
  dropoff: string;
  status: 'pending' | 'driver_arriving' | 'in_progress';
  scheduledTime?: Date;
  driver?: {
    name: string;
    initials: string;
    car: string;
    plate: string;
    rating?: number;
  };
  passenger?: {
    name: string;
    initials: string;
  };
}

export const PASSENGER_CANCEL_REASONS: CancelReason[] = [
  { id: 'driver_late', label: 'Driver is taking too long', requiresExplanation: false },
  { id: 'changed_plans', label: 'I changed my plans', requiresExplanation: false },
  { id: 'driver_asked', label: 'Driver asked me to cancel', requiresExplanation: false },
  { id: 'wrong_location', label: 'Wrong pickup location', requiresExplanation: false },
  { id: 'other', label: 'Other reason', requiresExplanation: true },
];

export const DRIVER_CANCEL_REASONS: CancelReason[] = [
  {
    id: 'passenger_not_found',
    label: 'Passenger not at pickup location',
    requiresExplanation: false,
  },
  { id: 'health_issue', label: 'Health issue - need to end shift', requiresExplanation: true },
  { id: 'vehicle_problem', label: 'Vehicle breakdown or issue', requiresExplanation: true },
  { id: 'safety_concern', label: 'Safety concern', requiresExplanation: true },
  { id: 'wrong_address', label: 'Cannot reach pickup address', requiresExplanation: false },
  { id: 'other', label: 'Other reason', requiresExplanation: true },
];
