import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ProfileData, VehicleInfo } from '../models/profile.model';
import { environment } from '../../../../environments/environment';

interface VehicleInfoResponse {
  id: number;
  model: string | null;
  vehicleTypeName: string;
  licensePlate: string | null;
  numSeats: number;
  babyFriendly: boolean;
  petFriendly: boolean;
}

interface ProfileResponse {
  id: number;
  name: string;
  surname: string;
  email: string;
  address: string;
  phone: string;
  profilePictureUrl: string | null;
  blocked: boolean;
  role: 'PASSENGER' | 'DRIVER' | 'ADMIN';
  createdAt: string;
  updatedAt: string;
  // Driver-specific fields
  activeDriver?: boolean;
  lastStateChangeAt?: string;
  vehicle?: VehicleInfoResponse;
}

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/profile`;

  getProfile(): Observable<ProfileData> {
    return this.http.get<ProfileResponse>(this.apiUrl).pipe(map((response) => this.mapToProfileData(response)));
  }

  updateProfile(
    updates: {
      name?: string;
      surname?: string;
      email?: string;
      address?: string;
      phone?: string;
      profilePictureUrl?: string;
    },
  ): Observable<ProfileData> {
    return this.http.put<ProfileResponse>(this.apiUrl, updates).pipe(map((response) => this.mapToProfileData(response)));
  }

  private mapToProfileData(response: ProfileResponse): ProfileData {
    const baseProfile: ProfileData = {
      id: response.id,
      firstName: response.name,
      lastName: response.surname,
      email: response.email,
      phone: response.phone || '',
      address: response.address || '',
      role: response.role,
      avatarUrl: response.profilePictureUrl,
    };

    // Add driver-specific data if available
    if (response.role === 'DRIVER' && response.vehicle) {
      const vehicleInfo: VehicleInfo = {
        model: response.vehicle.model || 'Vehicle',
        category: this.mapVehicleType(response.vehicle.vehicleTypeName),
        licensePlate: response.vehicle.licensePlate || '',
        seats: response.vehicle.numSeats,
        features: {
          babySeats: response.vehicle.babyFriendly,
          petFriendly: response.vehicle.petFriendly,
        },
      };

      return {
        ...baseProfile,
        vehicle: vehicleInfo,
        activeHoursLast24h: undefined, // Not implemented in backend yet
      };
    }

    return baseProfile;
  }

  private mapVehicleType(typeName: string): 'Standard' | 'Luxury' | 'Van' {
    const typeMap: Record<string, 'Standard' | 'Luxury' | 'Van'> = {
      STANDARD: 'Standard',
      LUXURY: 'Luxury',
      VAN: 'Van',
    };
    return typeMap[typeName] || 'Standard';
  }
}
