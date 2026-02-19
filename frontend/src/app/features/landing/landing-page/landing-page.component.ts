import { Component, signal, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { RideEstimatePanelComponent } from '../components/ride-estimate-panel/ride-estimate-panel.component';
import { MapComponent, MapConfig } from '../../map/map.component';
import { MapMarker } from '../../map/models/vehicle.model';
import {
  ActiveVehicleMarkersService,
  ACTIVE_VEHICLE_POLLING_INTERVAL_MS,
} from '../../map/services/active-vehicle-markers.service';

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
  private activeVehicleMarkersService = inject(ActiveVehicleMarkersService);

  ngOnInit(): void {
    this.startVehiclePolling();
  }

  /**
   * Starts polling for vehicle updates every VEHICLE_POLLING_INTERVAL_MS.
   * Uses real API or mock service based on environment configuration.
   * On error, keeps showing last known data and logs the error.
   */
  private startVehiclePolling(): void {
    this.activeVehicleMarkersService
      .streamMarkers(ACTIVE_VEHICLE_POLLING_INTERVAL_MS)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((markers) => this.vehicleMarkers.set(markers));
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
