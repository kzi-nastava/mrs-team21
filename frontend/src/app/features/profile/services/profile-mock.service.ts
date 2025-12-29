import { Injectable } from '@angular/core';
import { ProfileData } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileMockService {
  getProfile(): ProfileData {
    return {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      phone: '+381 64 123 4567',
      address: '123 Main Street, Belgrade, Serbia',

      role: 'DRIVER',
      avatarUrl: null,

      pendingChanges: false,

      activeHoursLast24h: {
        label: 'Active hours (Last 24h)',
        value: '6h 45m',
        percentOfLimit: 84,
        dailyLimitHours: 8,
        dailyLimitLabel: '84% of daily limit (8 hours)',
      },

      vehicle: {
        model: 'Tesla Model 3',
        category: 'Luxury Sedan',
        licensePlate: 'BG-123-AB',
        seats: 4,
        features: {
          babySeats: true,
          petFriendly: true,
        },
      },
    };
  }
}
