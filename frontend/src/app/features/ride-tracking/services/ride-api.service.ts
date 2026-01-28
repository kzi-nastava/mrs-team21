import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PageResponse, RideResponseDto } from '../../ride-history/models/ride-api.model';

/** Response from creating/fetching a ride inconsistency report */
export interface RideInconsistencyResponse {
  id: number;
  rideId: number;
  passengerId: number;
  passengerName: string;
  passengerSurname: string;
  note: string;
  createdAt: string;
}

/** Request body for creating a ride inconsistency report */
export interface RideInconsistencyCreateRequest {
  note: string;
}

@Injectable({ providedIn: 'root' })
export class RideApiService {
  private readonly http = inject(HttpClient);

  endRide(rideId: number): Observable<RideResponseDto> {
    return this.http.put<RideResponseDto>(`${environment.apiBaseUrl}/rides/${rideId}/end`, null);
  }

  getUpcomingDriverRides(driverId: number, page = 0, size = 5): Observable<RideResponseDto[]> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'scheduledFor,asc');

    return this.http
      .get<PageResponse<RideResponseDto>>(
        `${environment.apiBaseUrl}/drivers/${driverId}/rides/upcoming`,
        { params },
      )
      .pipe(map((response) => response.content));
  }

  /**
   * Report a driver route inconsistency for an active ride.
   * The passenger ID is automatically extracted from the JWT token on the backend.
   *
   * @param rideId - The ID of the ride to report inconsistency for
   * @param note - Description of the inconsistency (min 10, max 1000 chars)
   * @returns Observable of the created inconsistency report
   */
  reportInconsistency(rideId: number, note: string): Observable<RideInconsistencyResponse> {
    const request: RideInconsistencyCreateRequest = { note };
    return this.http.post<RideInconsistencyResponse>(
      `${environment.apiBaseUrl}/rides/${rideId}/inconsistencies`,
      request,
    );
  }

  /**
   * Get all inconsistency reports for a ride.
   *
   * @param rideId - The ID of the ride
   * @returns Observable of inconsistency reports
   */
  getInconsistencies(rideId: number): Observable<RideInconsistencyResponse[]> {
    return this.http.get<RideInconsistencyResponse[]>(
      `${environment.apiBaseUrl}/rides/${rideId}/inconsistencies`,
    );
  }
}
