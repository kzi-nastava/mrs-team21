import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface DriverRegistrationRequest {
  name: string;
  surname: string;
  email: string;
  address: string;
  phone: string;
  profilePictureUrl?: string | null;
  vehicleTypeId: number;
  vehicleModel: string;
  vehicleLicensePlate: string;
  vehicleNumSeats: number;
  vehicleBabyFriendly: boolean;
  vehiclePetFriendly: boolean;
}

export interface DriverRegistrationResponse {
  id: number;
  name: string;
  surname: string;
  email: string;
  address: string;
  phone: string;
  profilePictureUrl: string | null;
  blocked: boolean;
  activeDriver: boolean;
  isBusy: boolean;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class DriverRegistrationService {
  private apiUrl = `${environment.apiBaseUrl}/drivers`;

  constructor(private http: HttpClient) {}

  registerDriver(request: DriverRegistrationRequest): Observable<DriverRegistrationResponse> {
    // Guard against indefinite spinner if backend gets stuck.
    return this.http.post<DriverRegistrationResponse>(this.apiUrl, request).pipe(timeout(15000));
  }

  /**
   * Map vehicle category string to vehicle type ID
   * Standard -> 1, Luxury -> 2, Van -> 3
   */
  mapCategoryToTypeId(category: string): number {
    const categoryMap: { [key: string]: number } = {
      Standard: 1,
      Luxury: 2,
      Van: 3,
    };
    return categoryMap[category] || 1;
  }
}
