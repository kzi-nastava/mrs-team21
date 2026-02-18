import { Injectable, inject } from '@angular/core';
import { Observable, timer, map, switchMap, takeWhile, of, tap, catchError } from 'rxjs';
import { RideApiService } from './ride-api.service';
import { ActiveRide, LocationUpdate, RideInconsistencyReport } from '../models/active-ride.model';

const POLL_INTERVAL_MS = 3000;

/**
 * Ride tracking using real backend: GET /rides/:id and optional tracking-demo.
 * Maps RideTrackingResponse to ActiveRide and polls for location updates.
 */
@Injectable({ providedIn: 'root' })
export class RideTrackingApiService {
  private readonly rideApi = inject(RideApiService);

  getActiveRide(rideId: string): Observable<ActiveRide | null> {
    const id = Number(rideId);
    if (!Number.isInteger(id)) {
      return of(null);
    }
    return this.rideApi.getRideForTracking(id).pipe(
      tap((dto) => {
        if (dto.status === 'ACTIVE' && dto.waypoints?.length >= 2) {
          this.rideApi.startTrackingDemo(id).pipe(catchError(() => of(void 0))).subscribe();
        }
      }),
      map((dto) => this.mapToActiveRide(dto)),
    );
  }

  getVehicleLocationUpdates(rideId: string): Observable<LocationUpdate> {
    const id = Number(rideId);
    if (!Number.isInteger(id)) {
      return of();
    }
    return timer(0, POLL_INTERVAL_MS).pipe(
      switchMap(() => this.rideApi.getRideForTracking(id)),
      map((dto) => ({ update: this.dtoToLocationUpdate(dto), status: dto.status })),
      takeWhile((x) => x.status === 'ACTIVE', true),
      map((x) => x.update),
    );
  }

  reportInconsistency(
    _rideId: string,
    note: string,
    _reportedBy: { firstName: string; lastName: string; email: string },
  ): Observable<RideInconsistencyReport> {
    return this.rideApi.reportInconsistency(Number(_rideId), note).pipe(
      map((res) => ({
        id: String(res.id),
        rideId: String(res.rideId),
        note: res.note,
        reportedAt: new Date(res.createdAt),
        reportedBy: {
          firstName: res.passengerName,
          lastName: res.passengerSurname,
          email: '',
        },
      })),
    );
  }

  getInconsistencyReports(_rideId: string): RideInconsistencyReport[] {
    return [];
  }

  private mapToActiveRide(dto: import('./ride-api.service').RideTrackingResponseDto): ActiveRide {
    const waypoints = (dto.waypoints ?? []).slice().sort((a, b) => a.order - b.order);
    const start = waypoints[0];
    const dest = waypoints[waypoints.length - 1];
    const startLocation = start ? { lat: start.lat, lng: start.lng } : { lat: 0, lng: 0 };
    const destinationLocation = dest ? { lat: dest.lat, lng: dest.lng } : { lat: 0, lng: 0 };
    const currentLat = dto.vehicleCurrentLat ?? start?.lat ?? startLocation.lat;
    const currentLng = dto.vehicleCurrentLng ?? start?.lng ?? startLocation.lng;
    const etaSec = dto.estimatedDurationSec ?? 0;

    return {
      id: String(dto.id),
      status: dto.status,
      startAddress: start?.address ?? 'Pickup',
      destinationAddress: dest?.address ?? 'Destination',
      startLocation,
      destinationLocation,
      currentLocation: { lat: currentLat, lng: currentLng },
      driver: {
        id: dto.driverId ?? 0,
        firstName: dto.driverName ?? 'Driver',
        lastName: dto.driverSurname ?? '',
        phone: '',
      },
      vehicle: {
        id: dto.vehicleId ?? 0,
        model: dto.vehicleModel ?? '',
        licensePlate: dto.vehicleLicensePlate ?? '',
        vehicleType: 'STANDARD',
      },
      estimatedArrivalTime: etaSec,
      startTime: dto.startTime ? new Date(dto.startTime) : new Date(),
      route: waypoints.map((w) => ({ lat: w.lat, lng: w.lng, order: w.order })),
    };
  }

  private dtoToLocationUpdate(dto: import('./ride-api.service').RideTrackingResponseDto): LocationUpdate {
    const lat = dto.vehicleCurrentLat ?? (dto.waypoints?.[0]?.lat ?? 0);
    const lng = dto.vehicleCurrentLng ?? (dto.waypoints?.[0]?.lng ?? 0);
    const etaSec = dto.estimatedDurationSec ?? 0;
    return {
      lat,
      lng,
      timestamp: new Date(),
      estimatedArrivalTime: etaSec,
    };
  }
}
