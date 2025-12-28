export type UserRole = 'PASSENGER' | 'DRIVER' | 'ADMIN';

export interface VehicleFeatures {
  babySeats: boolean;
  petFriendly: boolean;
}

export interface VehicleInfo {
  model: string;
  category: string;
  licensePlate: string;
  seats: number;
  features: VehicleFeatures;
}

export interface ActiveHoursInfo {
  label: string;
  value: string;
  dailyLimitLabel: string;
  percentOfLimit: number; // 0..100
  dailyLimitHours: number;
}

export interface ProfileData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;

  role: UserRole;

  avatarUrl?: string | null;

  // Driver-only
  activeHoursLast24h?: ActiveHoursInfo;
  vehicle?: VehicleInfo;

  // Driver-only: profile changes waiting admin approval
  pendingChanges?: boolean;
}
