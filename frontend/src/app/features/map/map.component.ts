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
  @Input() showRoute = false;
  @Input() routeCoordinates?: [number, number][]; // [lng, lat] pairs
  @Input() routeColor = '#5b4cdb';
  @Input() routeWidth = 4;
  @Input() useCarIcon = false;
  @Input() carBearing?: number; // Bearing in degrees

  @Output() mapReady = new EventEmitter<mapboxgl.Map>();
  @Output() mapClick = new EventEmitter<{ lat: number; lng: number }>();

  private map!: mapboxgl.Map;
  private markerInstances: mapboxgl.Marker[] = [];
  private routeSourceId = 'route';
  private routeLayerId = 'route-line';

  ngAfterViewInit(): void {
    this.initializeMap();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Update markers when input changes (after map is initialized)
    if (changes['markers'] && !changes['markers'].firstChange && this.map) {
      this.addMarkers();
    }

    // Update route when route coordinates change
    if ((changes['routeCoordinates'] || changes['showRoute']) && this.map) {
      if (this.showRoute && this.routeCoordinates && this.routeCoordinates.length >= 2) {
        // Wait for map to be ready if it's still loading
        if (this.map.loaded()) {
          this.drawRoute(this.routeCoordinates);
        } else {
          this.map.once('load', () => {
            this.drawRoute(this.routeCoordinates!);
          });
        }
      } else {
        this.clearRoute();
      }
    }

    // Update car bearing when it changes
    if (
      changes['carBearing'] &&
      !changes['carBearing']?.firstChange &&
      this.map &&
      this.useCarIcon
    ) {
      this.updateCarBearing();
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
      if (this.showRoute && this.routeCoordinates && this.routeCoordinates.length >= 2) {
        this.drawRoute(this.routeCoordinates);
      } else {
        this.clearRoute();
      }
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
      const isCarIcon = this.useCarIcon && marker.status === 'busy';

      const mapboxMarker = new mapboxgl.Marker({
        element: el,
        anchor: isCarIcon ? 'center' : 'bottom',
      })
        .setLngLat([marker.lng, marker.lat])
        .addTo(this.map);

      // Add popup with driver info if available (using DOM methods to prevent XSS)
      // Skip popup for car icon to keep it clean
      if (marker.driverName && !isCarIcon) {
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
    // Use car icon if enabled and this is the vehicle marker (status: busy)
    if (this.useCarIcon && marker.status === 'busy') {
      return this.createCarIcon(this.carBearing);
    }

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

  private createCarIcon(bearing?: number): HTMLElement {
    const el = document.createElement('div');
    el.className = 'car-icon-marker';

    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.classList.add('car-icon-svg');
    svg.setAttribute('viewBox', '0 0 15 15');
    svg.setAttribute('width', '24');
    svg.setAttribute('height', '24');
    svg.setAttribute('fill', 'currentColor');

    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute(
      'd',
      'M13.84,6.852,12.6,5.7,11.5,3.5a1.05,1.05,0,0,0-.9-.5H4.4a1.05,1.05,0,0,0-.9.5L2.4,5.7,1.16,6.852A.5.5,0,0,0,1,7.219V11.5a.5.5,0,0,0,.5.5h2c.2,0,.5-.2.5-.4V11h7v.5c0,.2.2.5.4.5h2.1a.5.5,0,0,0,.5-.5V7.219A.5.5,0,0,0,13.84,6.852ZM4.5,4h6l1,2h-8ZM5,8.6c0,.2-.3.4-.5.4H2.4C2.2,9,2,8.7,2,8.5V7.4c.1-.3.3-.5.6-.4l2,.4c.2,0,.4.3.4.5Zm8-.1c0,.2-.2.5-.4.5H10.5c-.2,0-.5-.2-.5-.4V7.9c0-.2.2-.5.4-.5l2-.4c.3-.1.5.1.6.4Z'
    );
    svg.appendChild(path);

    // Apply rotation to icon if bearing provided
    if (bearing !== undefined) {
      svg.style.transform = `rotate(${bearing}deg)`;
      svg.style.transformOrigin = 'center center';
    }

    el.appendChild(svg);

    return el;
  }

  private updateCarBearing(): void {
    // Update bearing for car icon markers
    this.markerInstances.forEach((marker, index) => {
      const markerData = this.markers[index];
      if (markerData && markerData.status === 'busy' && this.useCarIcon) {
        const element = marker.getElement();
        if (element) {
          const icon = element.querySelector('.car-icon-svg') as HTMLElement | null;
          if (icon && this.carBearing !== undefined) {
            icon.style.transform = `rotate(${this.carBearing}deg)`;
            icon.style.transformOrigin = 'center center';
          }
        }
      }
    });
  }

  drawRoute(coordinates: [number, number][]): void {
    if (!this.map || !coordinates || coordinates.length < 2) {
      return;
    }

    const routeGeoJSON = {
      type: 'Feature' as const,
      geometry: {
        type: 'LineString' as const,
        coordinates: coordinates, // Already in [lng, lat] format
      },
      properties: {},
    };

    // Remove existing route if any
    if (this.map.getSource(this.routeSourceId)) {
      if (this.map.getLayer(this.routeLayerId)) {
        this.map.removeLayer(this.routeLayerId);
      }
      this.map.removeSource(this.routeSourceId);
    }

    this.map.addSource(this.routeSourceId, {
      type: 'geojson',
      data: routeGeoJSON as GeoJSON.Feature<GeoJSON.LineString>,
    });

    this.map.addLayer({
      id: this.routeLayerId,
      type: 'line',
      source: this.routeSourceId,
      layout: {
        'line-join': 'round',
        'line-cap': 'round',
      },
      paint: {
        'line-color': this.routeColor,
        'line-width': this.routeWidth,
        'line-opacity': 0.75,
      },
    });
  }

  clearRoute(): void {
    if (!this.map) {
      return;
    }

    if (this.map.getLayer(this.routeLayerId)) {
      this.map.removeLayer(this.routeLayerId);
    }
    if (this.map.getSource(this.routeSourceId)) {
      this.map.removeSource(this.routeSourceId);
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.clearRoute();
      this.map.remove();
    }
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
