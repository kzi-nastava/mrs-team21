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
  /** Hours driven in the last 24h; present for drivers when backend supports it. */
  activeHoursLast24h?: { hoursWorked: number; maxHours: number };
}

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/profile`;
  private apiOrigin = environment.apiBaseUrl.replace(/\/api\/?$/, '');

  getProfile(): Observable<ProfileData> {
    return this.http.get<ProfileResponse>(this.apiUrl).pipe(map((response) => this.mapToProfileData(response)));
  }

  /**
   * Upload profile picture file. Returns the URL path; then call updateProfile({ profilePictureUrl: response.url }).
   */
  uploadProfilePicture(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.set('file', file);
    return this.http.post<{ url: string }>(`${this.apiUrl}/picture`, formData);
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
      avatarUrl: this.resolveProfilePictureUrl(response.profilePictureUrl),
    };

    // Add driver-specific data if available
    if (response.role === 'DRIVER') {
      const driverProfile: ProfileData = {
        ...baseProfile,
        activeHoursLast24h: response.activeHoursLast24h ?? undefined,
      };
      if (response.vehicle) {
        driverProfile.vehicle = {
          model: response.vehicle.model || 'Vehicle',
          category: this.mapVehicleType(response.vehicle.vehicleTypeName),
          licensePlate: response.vehicle.licensePlate || '',
          seats: response.vehicle.numSeats,
          features: {
            babySeats: response.vehicle.babyFriendly,
            petFriendly: response.vehicle.petFriendly,
          },
        };
      }
      return driverProfile;
    }

    return baseProfile;
  }

  /** Resolve relative profile picture URL to absolute for display. */
  private resolveProfilePictureUrl(url: string | null | undefined): string | null {
    if (url == null || url === '') return null;
    if (url.startsWith('http') || url.startsWith('data:')) return url;
    const path = url.startsWith('/') ? url : '/' + url;
    return this.apiOrigin + path;
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
