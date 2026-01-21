import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  inject,
  DestroyRef,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MapComponent, MapConfig } from '../map/map.component';
import { MapMarker } from '../map/models/vehicle.model';
import { RideTrackingMockService } from './services/ride-tracking-mock.service';
import { ActiveRide, LocationUpdate } from './models/active-ride.model';

@Component({
  selector: 'app-ride-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MapComponent],
  templateUrl: './ride-tracking.component.html',
  styleUrl: './ride-tracking.component.scss',
})
export class RideTrackingComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MapComponent) mapComponent!: MapComponent;

  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);
  private rideTrackingService = inject(RideTrackingMockService);

  rideId = signal<string>('');
  activeRide = signal<ActiveRide | null>(null);
  currentLocation = signal<{ lat: number; lng: number } | null>(null);
  etaSeconds = signal<number>(0);
  markers = signal<MapMarker[]>([]);
  showInconsistencyForm = signal<boolean>(false);
  inconsistencyNote = signal<string>('');
  isSubmittingReport = signal<boolean>(false);
  reportSubmitted = signal<boolean>(false);

  mapConfig: MapConfig = {
    center: [19.8335, 45.2671], // Novi Sad center [lng, lat]
    zoom: 13,
  };

  ngOnInit(): void {
    const rideIdParam = this.route.snapshot.paramMap.get('rideId');
    if (rideIdParam) {
      this.rideId.set(rideIdParam);
      this.loadActiveRide(rideIdParam);
    }
  }

  ngAfterViewInit(): void {
    // Wait a bit for map to initialize
    setTimeout(() => {
      if (this.mapComponent && this.currentLocation()) {
        this.updateMapView();
      }
    }, 500);
  }

  ngOnDestroy(): void {
    // Cleanup handled by takeUntilDestroyed
  }

  private loadActiveRide(rideId: string): void {
    this.rideTrackingService
      .getActiveRide(rideId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ride) => {
          this.activeRide.set(ride);
          this.currentLocation.set(ride.currentLocation);
          this.etaSeconds.set(ride.estimatedArrivalTime);
          this.updateMarkers();
          this.startLocationUpdates(rideId);
        },
        error: (error) => {
          console.error('Error loading active ride:', error);
        },
      });
  }

  private startLocationUpdates(rideId: string): void {
    this.rideTrackingService
      .getVehicleLocationUpdates(rideId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (update: LocationUpdate) => {
          this.currentLocation.set({ lat: update.lat, lng: update.lng });
          this.etaSeconds.set(update.estimatedArrivalTime);
          this.updateMarkers();
          this.updateMapView();
        },
        error: (error) => {
          console.error('Error receiving location updates:', error);
        },
      });
  }

  private updateMarkers(): void {
    const ride = this.activeRide();
    const currentLoc = this.currentLocation();

    if (!ride || !currentLoc) return;

    const markers: MapMarker[] = [];

    // Vehicle marker (current position)
    markers.push({
      lat: currentLoc.lat,
      lng: currentLoc.lng,
      status: 'busy',
      driverName: `${ride.driver.firstName} ${ride.driver.lastName}`,
    });

    // Start marker (if we have start location)
    if (ride.startLocation) {
      markers.push({
        lat: ride.startLocation.lat,
        lng: ride.startLocation.lng,
        status: 'available',
      });
    }

    // Destination marker (if we have destination location)
    if (ride.destinationLocation) {
      markers.push({
        lat: ride.destinationLocation.lat,
        lng: ride.destinationLocation.lng,
        status: 'available',
      });
    }

    this.markers.set(markers);
  }

  private updateMapView(): void {
    const currentLoc = this.currentLocation();
    if (currentLoc && this.mapComponent) {
      this.mapComponent.setView(currentLoc.lat, currentLoc.lng, 13);
    }
  }

  formatETA(seconds: number): string {
    if (seconds <= 0) {
      return 'Arrived';
    }

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    if (minutes === 0) {
      return `${remainingSeconds} sec`;
    } else if (remainingSeconds === 0) {
      return `${minutes} min`;
    } else {
      return `${minutes} min ${remainingSeconds} sec`;
    }
  }

  openInconsistencyForm(): void {
    this.showInconsistencyForm.set(true);
    this.reportSubmitted.set(false);
    this.inconsistencyNote.set('');
  }

  closeInconsistencyForm(): void {
    this.showInconsistencyForm.set(false);
    this.inconsistencyNote.set('');
  }

  submitInconsistencyReport(): void {
    const note = this.inconsistencyNote().trim();
    if (!note || note.length < 10) {
      return; // Basic validation
    }

    const ride = this.activeRide();
    if (!ride) return;

    this.isSubmittingReport.set(true);

    // Mock user data - in real app, get from auth service
    const reportedBy = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
    };

    this.rideTrackingService
      .reportInconsistency(ride.id, note, reportedBy)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.reportSubmitted.set(true);
          this.isSubmittingReport.set(false);
          setTimeout(() => {
            this.closeInconsistencyForm();
          }, 2000);
        },
        error: (error) => {
          console.error('Error submitting report:', error);
          this.isSubmittingReport.set(false);
        },
      });
  }
}
