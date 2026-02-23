import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, Subscription, timer } from 'rxjs';
import { catchError, exhaustMap, filter } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { VehicleApiService } from '../../features/map/services/vehicle-api.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class DriverLocationPingService {
  private readonly authService = inject(AuthService);
  private readonly vehicleApiService = inject(VehicleApiService);

  private pingSubscription: Subscription | null = null;
  private hasLoggedUnsupportedGeolocation = false;
  private hasLoggedPermissionDenied = false;

  start(): void {
    if (this.pingSubscription || !this.shouldPing()) {
      return;
    }

    if (!navigator.geolocation) {
      if (!this.hasLoggedUnsupportedGeolocation) {
        console.warn('Geolocation API is not available in this browser.');
        this.hasLoggedUnsupportedGeolocation = true;
      }
      return;
    }

    this.pingSubscription = timer(0, environment.driverLocationPingIntervalMs)
      .pipe(
        filter(() => this.shouldPing()),
        exhaustMap(() =>
          this.getCurrentPosition().pipe(
            catchError((error: unknown) => {
              this.handleGeolocationError(error);
              return EMPTY;
            }),
          ),
        ),
        exhaustMap((coords) =>
          this.vehicleApiService.updateMyLocation(coords.latitude, coords.longitude).pipe(
            catchError((error) => {
              console.warn('Driver location ping failed.', error);
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe();
  }

  stop(): void {
    this.pingSubscription?.unsubscribe();
    this.pingSubscription = null;
  }

  private shouldPing(): boolean {
    return this.authService.isAuthenticated() && this.authService.isDriver();
  }

  private getCurrentPosition(): Observable<{ latitude: number; longitude: number }> {
    return new Observable((observer) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          observer.next({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
          observer.complete();
        },
        (error) => observer.error(error),
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0,
        },
      );
    });
  }

  private handleGeolocationError(error: unknown): void {
    const geolocationError = error as GeolocationPositionError;
    if (geolocationError?.code === geolocationError.PERMISSION_DENIED) {
      if (!this.hasLoggedPermissionDenied) {
        console.warn('Location permission denied. Driver location pinging is disabled.');
        this.hasLoggedPermissionDenied = true;
      }
      this.stop();
      return;
    }

    console.warn('Unable to read current driver location.', error);
  }
}
