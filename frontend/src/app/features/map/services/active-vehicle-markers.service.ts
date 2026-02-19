import { Injectable, inject } from '@angular/core';
import { interval, Observable, of } from 'rxjs';
import { catchError, map, scan, startWith, switchMap } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { MapMarker, vehicleToMapMarker, VehicleResponse } from '../models/vehicle.model';
import { VehicleApiService } from './vehicle-api.service';
import { VehicleMockService } from './vehicle-mock.service';

export const ACTIVE_VEHICLE_POLLING_INTERVAL_MS = 10_000;

@Injectable({ providedIn: 'root' })
export class ActiveVehicleMarkersService {
  private readonly vehicleApiService = inject(VehicleApiService);
  private readonly vehicleMockService = inject(VehicleMockService);

  /**
   * Poll active vehicles and map them to map markers.
   * Keeps last non-empty markers to avoid map flicker on transient API empties/errors.
   */
  streamMarkers(
    pollIntervalMs = ACTIVE_VEHICLE_POLLING_INTERVAL_MS,
  ): Observable<MapMarker[]> {
    return interval(pollIntervalMs).pipe(
      startWith(0),
      switchMap(() => this.fetchMarkers()),
      scan((previousMarkers: MapMarker[], nextMarkers: MapMarker[] | null) => {
        if (nextMarkers === null) {
          return previousMarkers;
        }
        if (nextMarkers.length === 0 && previousMarkers.length > 0) {
          return previousMarkers;
        }
        return nextMarkers;
      }, [] as MapMarker[]),
    );
  }

  private fetchMarkers(): Observable<MapMarker[] | null> {
    const source$ = environment.useMockVehicles
      ? this.vehicleMockService.getActiveVehicles()
      : this.vehicleApiService.getActiveVehicles();

    return source$.pipe(
      map((vehicles: VehicleResponse[]) => vehicles.map((vehicle) => vehicleToMapMarker(vehicle))),
      catchError((error) => {
        console.error('Error fetching active vehicles:', error);
        return of(null);
      }),
    );
  }
}
