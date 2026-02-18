import { Component, signal, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { interval, switchMap, startWith, catchError, of } from 'rxjs';
import { RideEstimatePanelComponent } from '../components/ride-estimate-panel/ride-estimate-panel.component';
import { MapComponent, MapConfig } from '../../map/map.component';
import { VehicleMockService } from '../../map/services/vehicle-mock.service';
import { VehicleApiService } from '../../map/services/vehicle-api.service';
import { MapMarker, vehicleToMapMarker, VehicleResponse } from '../../map/models/vehicle.model';
import { environment } from '../../../../environments/environment';

/** Polling interval for vehicle updates in milliseconds */
const VEHICLE_POLLING_INTERVAL_MS = 10_000;

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink, RideEstimatePanelComponent, MapComponent],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent implements OnInit {
  isPanelOpen = signal(false);
  vehicleMarkers = signal<MapMarker[]>([]);
  routeCoordinates = signal<[number, number][] | undefined>(undefined);
  showRoute = signal(false);
  mapConfig: MapConfig = {
    center: [19.8200, 45.2500], // Novi Sad [lng, lat] - Mapbox uses lng,lat order
    zoom: 12.5,
  };

  private destroyRef = inject(DestroyRef);
  private vehicleApiService = inject(VehicleApiService);
  private vehicleMockService = inject(VehicleMockService);

  ngOnInit(): void {
    this.startVehiclePolling();
  }

  /**
   * Starts polling for vehicle updates every VEHICLE_POLLING_INTERVAL_MS.
   * Uses real API or mock service based on environment configuration.
   * On error, keeps showing last known data and logs the error.
   */
  private startVehiclePolling(): void {
    interval(VEHICLE_POLLING_INTERVAL_MS)
      .pipe(
        startWith(0), // Emit immediately on subscribe
        switchMap(() => this.fetchVehicles()),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (vehicles) => {
          if (vehicles.length > 0 || this.vehicleMarkers().length === 0) {
            const markers = vehicles.map((v) => vehicleToMapMarker(v));
            this.vehicleMarkers.set(markers);
          }
          // If API returns empty but we have markers, keep showing them
        },
        error: (error) => {
          console.error('Error in vehicle polling:', error);
        },
      });
  }

  /**
   * Fetches vehicles from API or mock service based on environment config.
   * Catches errors and returns empty array to prevent breaking the polling stream.
   */
  private fetchVehicles() {
    const source$ = environment.useMockVehicles
      ? this.vehicleMockService.getActiveVehicles()
      : this.vehicleApiService.getActiveVehicles();

    return source$.pipe(
      catchError((error) => {
        console.error('Error fetching active vehicles:', error);
        return of([] as VehicleResponse[]);
      })
    );
  }

  openPanel(): void {
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.isPanelOpen.set(false);
  }

  onRouteReady(coords: [number, number][] | undefined): void {
    this.routeCoordinates.set(coords ?? undefined);
    this.showRoute.set(Array.isArray(coords) && coords.length >= 2);
  }
}
