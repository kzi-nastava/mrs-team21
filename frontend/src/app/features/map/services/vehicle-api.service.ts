import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { VehicleResponse } from '../models/vehicle.model';

/**
 * Service for fetching vehicle data from the backend API.
 * Used for displaying active vehicles on the landing page map.
 */
@Injectable({ providedIn: 'root' })
export class VehicleApiService {
  private readonly http = inject(HttpClient);

  /**
   * Fetches all active vehicles (vehicles from drivers with activeDriver=true).
   * Returns both available (free) and busy (on ride) vehicles.
   * This endpoint is public and doesn't require authentication.
   */
  getActiveVehicles(): Observable<VehicleResponse[]> {
    return this.http.get<VehicleResponse[]>(
      `${environment.apiBaseUrl}/vehicles/active`
    );
  }
}
