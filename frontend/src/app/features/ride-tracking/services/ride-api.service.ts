import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { map, Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { PageResponse, RideResponseDto } from '../../ride-history/models/ride-api.model';

/** Response from GET /rides/me/active */
export interface ActiveRideIdResponse {
  rideId: number;
}

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

/** Request body for stopping a ride (driver stops ride early) */
export interface RideStopRequest {
  stopAddress: string;
  stopLat: number;
  stopLng: number;
}

/** Response from GET /rides/:id (tracking). Maps to RideTrackingResponse.java */
export interface RideTrackingResponseDto {
  id: number;
  status: string;
  requestedAt: string;
  startTime: string | null;
  driverId: number | null;
  driverName: string | null;
  driverSurname: string | null;
  vehicleId: number | null;
  vehicleModel: string | null;
  vehicleLicensePlate: string | null;
  vehicleCurrentLat: number | null;
  vehicleCurrentLng: number | null;
  waypoints: { locationId: number; address: string; lat: number; lng: number; order: number }[];
  estimatedArrivalAt: string | null;
  estimatedDurationSec: number | null;
  totalDistanceKm: number | null;
  babyTransport: boolean | null;
  petTransport: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class RideApiService {
  private readonly http = inject(HttpClient);

  /** Get current user's active ride id for tracking (passenger or driver). Returns null if 404. */
  getMyActiveRide(): Observable<ActiveRideIdResponse | null> {
    return this.http
      .get<ActiveRideIdResponse>(`${environment.apiBaseUrl}/rides/me/active`)
      .pipe(
        catchError((error: HttpErrorResponse) =>
          error.status === 404 ? of(null) : throwError(() => error),
        ),
      );
  }

  /** Get ride with tracking data (driver position, waypoints). Used by ride-tracking page. */
  getRideForTracking(rideId: number): Observable<RideTrackingResponseDto> {
    return this.http.get<RideTrackingResponseDto>(`${environment.apiBaseUrl}/rides/${rideId}`);
  }

  /** Start backend simulation of vehicle movement for demo (ride must be ACTIVE). */
  startTrackingDemo(rideId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/rides/${rideId}/tracking-demo/start`, {});
  }

  /** Stop backend simulation for this ride. */
  stopTrackingDemo(rideId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/rides/${rideId}/tracking-demo/stop`, {});
  }

  /** Start ride (driver action): transitions ACCEPTED -> ACTIVE. */
  startRide(rideId: number): Observable<RideResponseDto> {
    return this.http.put<RideResponseDto>(`${environment.apiBaseUrl}/rides/${rideId}/start`, null);
  }

  endRide(rideId: number): Observable<RideResponseDto> {
    return this.http.put<RideResponseDto>(`${environment.apiBaseUrl}/rides/${rideId}/end`, null);
  }

  getUpcomingDriverRides(driverId: number, page = 0, size = 5): Observable<RideResponseDto[]> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'scheduledFor,asc');

    return this.http
      .get<
        PageResponse<RideResponseDto>
      >(`${environment.apiBaseUrl}/drivers/${driverId}/rides/upcoming`, { params })
      .pipe(map((response) => response.content));
  }

  createPanic(rideId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/rides/${rideId}/panic`, {});
  }

  stopRide(rideId: number, request: RideStopRequest): Observable<RideResponseDto> {
    return this.http.put<RideResponseDto>(
      `${environment.apiBaseUrl}/rides/${rideId}/stop`,
      request,
    );
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
