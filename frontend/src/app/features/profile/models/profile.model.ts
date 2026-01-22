export type UserRole = 'PASSENGER' | 'DRIVER' | 'ADMIN';

export type VehicleCategory = 'Standard' | 'Luxury' | 'Van';

export interface VehicleFeatures {
  babySeats: boolean;
  petFriendly: boolean;
}

export interface VehicleInfo {
  model: string;
  category: VehicleCategory;
  licensePlate: string;
  seats: number;
  features: VehicleFeatures;
}

export interface ActiveHoursInfo {
  /** Number of hours worked in the last 24 hours */
  hoursWorked: number;
  /** Maximum allowed hours (always 8) */
  maxHours: number;
}

/** Form data for editing personal information */
export interface PersonalInfoForm {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
}

/** Form data for editing vehicle information */
export interface VehicleInfoForm {
  model: string;
  category: VehicleCategory;
  licensePlate: string;
  seats: number;
  babySeats: boolean;
  petFriendly: boolean;
}

/** Pending change request for driver profile updates */
export interface PendingChangeRequest {
  id: string;
  type: 'personal' | 'vehicle';
  requestedAt: Date;
  status: 'pending' | 'approved' | 'rejected';
  changes: Partial<PersonalInfoForm> | Partial<VehicleInfoForm>;
}

export interface ProfileData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;

  role: UserRole;

  avatarUrl?: string | null;

  // Driver-only fields
  activeHoursLast24h?: ActiveHoursInfo;
  vehicle?: VehicleInfo;
  
  // Pending change requests (driver only)
  pendingChanges?: PendingChangeRequest[];
}
