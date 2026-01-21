import { Component, signal, OnInit } from '@angular/core';
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
export class LandingPageComponent implements OnInit {
  isPanelOpen = signal(false);
  vehicleMarkers = signal<MapMarker[]>([]);
  mapConfig: MapConfig = {
    center: [19.8200, 45.2500], // Novi Sad [lng, lat] - Mapbox uses lng,lat order
    zoom: 12.5,
  };

  constructor(private vehicleService: VehicleMockService) {}

  ngOnInit(): void {
    this.loadVehicles();
  }

  private loadVehicles(): void {
    this.vehicleService.getActiveVehicles().subscribe((vehicles) => {
      const markers = vehicles.map((v) => vehicleToMapMarker(v));
      this.vehicleMarkers.set(markers);
    });
  }

  openPanel(): void {
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.isPanelOpen.set(false);
  }
}
