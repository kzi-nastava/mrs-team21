import { Passenger } from './passenger.model';

export interface Driver {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  rating?: number;
  photoUrl?: string;
}

export interface Ride {
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
  driver?: Driver;
  vehicleType?: 'STANDARD' | 'LUXURY' | 'VAN';
  status?: string;
  scheduledFor?: Date | null;
  requestedAt?: Date | null;
  
  // Rating fields
  driverRating?: number;
  vehicleRating?: number;
  /** Passenger's comment left when rating (if any) */
  reviewComment?: string | null;
  
  // Rating status fields (from backend rating-status endpoint)
  hasReview?: boolean;
  canRate?: boolean;
  daysRemainingToRate?: number;
  ratingDeadline?: Date;
  
  // TODO: Backend Integration - Link to RidePassenger.isFavorite
  // This should be: isFavorite: boolean; (per user per ride)
  // Currently local state only - Backend needs RidePassenger entity update
  isFavorite?: boolean;
}
