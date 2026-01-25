import { Component, signal, OnInit, OnDestroy, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { RouterLink } from '@angular/router';
import { RideEstimatePanelComponent } from '../components/ride-estimate-panel/ride-estimate-panel.component';
import { MapComponent, MapConfig } from '../../map/map.component';
import { VehicleMockService } from '../../map/services/vehicle-mock.service';
import { MapMarker, vehicleToMapMarker } from '../../map/models/vehicle.model';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink, RideEstimatePanelComponent, MapComponent],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent implements OnInit, OnDestroy {
  isPanelOpen = signal(false);
  vehicleMarkers = signal<MapMarker[]>([]);
  mapConfig: MapConfig = {
    center: [19.8200, 45.2500], // Novi Sad [lng, lat] - Mapbox uses lng,lat order
    zoom: 12.5,
  };

  private destroyRef = inject(DestroyRef);
  private vehicleService = inject(VehicleMockService);
  private subscription!: Subscription;

  ngOnInit(): void {
    this.loadVehicles();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.subscription.unsubscribe();
  }

  private loadVehicles(): void {
    this.subscription = this.vehicleService
      .getActiveVehicles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (vehicles) => {
          const markers = vehicles.map((v) => vehicleToMapMarker(v));
          this.vehicleMarkers.set(markers);
        },
        error: (error) => {
          console.error('Error loading active vehicles:', error);
        },
      });
  }

  openPanel(): void {
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.isPanelOpen.set(false);
  }
}
