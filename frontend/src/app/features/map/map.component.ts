import {
  Component,
  Input,
  OnDestroy,
  ElementRef,
  ViewChild,
  AfterViewInit,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import mapboxgl from 'mapbox-gl';
import { environment } from '../../../environments/environment';
import { MapMarker } from './models/vehicle.model';

export interface MapConfig {
  center: [number, number];
  zoom: number;
}

@Component({
  selector: 'app-map',
  standalone: true,
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
})
export class MapComponent implements AfterViewInit, OnDestroy, OnChanges {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  @Input() config: MapConfig = {
    center: [19.8200, 45.2500], // Novi Sad [lng, lat] - Mapbox uses lng,lat order
    zoom: 12.5,
  };

  @Input() markers: MapMarker[] = [];
  @Input() showControls = true;

  @Output() mapReady = new EventEmitter<mapboxgl.Map>();
  @Output() mapClick = new EventEmitter<{ lat: number; lng: number }>();

  private map!: mapboxgl.Map;
  private markerInstances: mapboxgl.Marker[] = [];

  ngAfterViewInit(): void {
    this.initializeMap();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Update markers when input changes (after map is initialized)
    if (changes['markers'] && !changes['markers'].firstChange && this.map) {
      this.addMarkers();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initializeMap(): void {
    // Validate Mapbox access token
    const token = environment.mapboxToken;
    if (!token || token === 'MAPBOX_API_KEY' || token.trim() === '') {
      console.error(
        'Mapbox API key is missing or invalid. Please create environment.dev.ts with your Mapbox API key.'
      );
      this.showMapError(
        'Mapbox API key is not configured. Please create environment.dev.ts with your Mapbox API key.'
      );
      return;
    }

    // Set Mapbox access token
    mapboxgl.accessToken = token;

    try {
      this.map = new mapboxgl.Map({
        container: this.mapContainer.nativeElement,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [this.config.center[0], this.config.center[1]], // [lng, lat]
        zoom: this.config.zoom,
        attributionControl: true,
      });
    } catch (error) {
      console.error('Failed to initialize Mapbox map:', error);
      this.showMapError('Failed to load map. Please check your Mapbox API key configuration.');
      return;
    }

    if (this.showControls) {
      this.map.addControl(new mapboxgl.NavigationControl(), 'top-left');
    }

    this.map.on('load', () => {
      this.addMarkers();
      this.mapReady.emit(this.map);
    });

    this.map.on('click', (e) => {
      this.mapClick.emit({ lat: e.lngLat.lat, lng: e.lngLat.lng });
    });
  }

  private addMarkers(): void {
    // Safety check: don't add markers if map isn't initialized yet
    if (!this.map) {
      return;
    }

    // Clear existing markers
    this.markerInstances.forEach((marker) => marker.remove());
    this.markerInstances = [];

    this.markers.forEach((marker) => {
      const el = this.createMarkerElement(marker);

      const mapboxMarker = new mapboxgl.Marker({
        element: el,
        anchor: 'bottom',
      })
        .setLngLat([marker.lng, marker.lat])
        .addTo(this.map);

      // Add popup with driver info if available (using DOM methods to prevent XSS)
      if (marker.driverName) {
        const popupContent = document.createElement('div');
        popupContent.className = 'popup-content';

        const driverNameElement = document.createElement('strong');
        driverNameElement.textContent = marker.driverName;
        popupContent.appendChild(driverNameElement);

        const statusElement = document.createElement('span');
        statusElement.className = `status ${marker.status}`;
        statusElement.textContent =
          marker.status === 'available' ? 'Available' : 'On a ride';
        popupContent.appendChild(statusElement);

        const popup = new mapboxgl.Popup({
          offset: 25,
          closeButton: false,
          className: 'driver-popup',
        }).setDOMContent(popupContent);
        mapboxMarker.setPopup(popup);
      }

      this.markerInstances.push(mapboxMarker);
    });
  }

  private createMarkerElement(marker: MapMarker): HTMLElement {
    const el = document.createElement('div');
    el.className = `vehicle-marker ${marker.status}`;

    // Create marker with pin shape and car icon
    el.innerHTML = `
      <div class="marker-pin">
        <div class="marker-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
          </svg>
        </div>
        <div class="pulse-ring"></div>
      </div>
    `;
    return el;
  }

  updateMarkers(markers: MapMarker[]): void {
    this.markers = markers;
    this.addMarkers();
  }

  setView(lat: number, lng: number, zoom?: number): void {
    this.map.flyTo({
      center: [lng, lat],
      zoom: zoom ?? this.map.getZoom(),
    });
  }

  getMap(): mapboxgl.Map {
    return this.map;
  }

  private showMapError(message: string): void {
    // Display error message in the map container
    if (this.mapContainer?.nativeElement) {
      const errorDiv = document.createElement('div');
      errorDiv.className = 'map-error';
      errorDiv.style.cssText =
        'padding: 20px; text-align: center; color: #ef4444; background: #fee2e2; border-radius: 8px; margin: 20px;';
      errorDiv.textContent = message;
      this.mapContainer.nativeElement.appendChild(errorDiv);
    }
  }
}
