import { Injectable } from '@angular/core';
import { ProfileData } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileMockService {
  /**
   * Returns mock profile data for testing.
   * 
   * TODO: Replace with actual API call
   * Example:
   * ```
   * getProfile(): Observable<ProfileData> {
   *   return this.http.get<ProfileData>('/api/profile');
   * }
   * ```
   * 
   * To test different roles, change the `role` field to 'PASSENGER' or 'DRIVER'
   */
  getProfile(): ProfileData {
    // Change this to 'PASSENGER' to test passenger view
    const testRole: 'DRIVER' | 'PASSENGER' = 'DRIVER';

    const baseProfile: ProfileData = {
      firstName: 'Marko',
      lastName: 'Petrović',
      email: 'marko.petrovic@example.com',
      phone: '+381 64 123 4567',
      address: 'Bulevar Oslobođenja 45, Novi Sad 21000, Serbia',
      role: testRole,
      avatarUrl: null, // Will use default avatar
    };

    // Add driver-specific data if role is DRIVER
    if (testRole === 'DRIVER') {
      return {
        ...baseProfile,
        activeHoursLast24h: {
          hoursWorked: 5.5, // 5 hours 30 minutes worked
          maxHours: 8,
        },
        vehicle: {
          model: 'Tesla Model 3',
          category: 'Luxury',
          licensePlate: 'NS-123-AB',
          seats: 4,
          features: {
            babySeats: true,
            petFriendly: false,
          },
        },
      };
    }

    return baseProfile;
  }
}
