import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  inject,
  DestroyRef,
  ViewChild,
  AfterViewInit,
  computed,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs';
import { MapComponent, MapConfig } from '../map/map.component';
import { MapMarker } from '../map/models/vehicle.model';
import { RideTrackingMockService } from './services/ride-tracking-mock.service';
import { MapboxDirectionsService } from './services/mapbox-directions.service';
import { ActiveRide, LocationUpdate } from './models/active-ride.model';
import { PanicComponent, PanicRideInfo } from './components/shared/panic/panic.component';
import { StopRideComponent, StopRideInfo } from './components/driver/stop-ride/stop-ride.component';
import { InconsistencyReportComponent } from './components/passenger/inconsistency-report/inconsistency-report.component';
import { RideApiService } from './services/ride-api.service';
import { RideResponseDto } from '../ride-history/models/ride-api.model';

@Component({
  selector: 'app-ride-tracking',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MapComponent,
    PanicComponent,
    StopRideComponent,
    InconsistencyReportComponent,
  ],
  templateUrl: './ride-tracking.component.html',
  styleUrl: './ride-tracking.component.scss',
})
export class RideTrackingComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MapComponent) mapComponent!: MapComponent;
  @ViewChild(InconsistencyReportComponent)
  inconsistencyReportComponent?: InconsistencyReportComponent;
  @ViewChild(PanicComponent)
  panicComponent?: PanicComponent;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private rideTrackingService = inject(RideTrackingMockService);
  private directionsService = inject(MapboxDirectionsService);
  private rideApiService = inject(RideApiService);

  private lastRouteRequestAt = 0;
  private routeRequestInFlight = false;
  private readonly minRerouteIntervalMs = 20000;

  rideId = signal<string>('');
  activeRide = signal<ActiveRide | null>(null);
  currentLocation = signal<{ lat: number; lng: number } | null>(null);
  etaSeconds = signal<number>(0);
  markers = signal<MapMarker[]>([]);
  routeCoordinates = signal<[number, number][] | undefined>(undefined);
  carBearing = signal<number | undefined>(undefined);
  showPanicModal = signal<boolean>(false);
  showStopModal = signal<boolean>(false);
  rideCompleted = signal<boolean>(false);
  endRideLoading = signal<boolean>(false);
  endRideError = signal<string | null>(null);
  upcomingRides = signal<RideResponseDto[]>([]);

  nextScheduledRide = computed(() => this.upcomingRides()[0] ?? null);

  nextRideSummary = computed(() => {
    const ride = this.nextScheduledRide();
    if (!ride) return null;
    const waypoints = [...(ride.waypoints ?? [])].sort((a, b) => a.order - b.order);
    return {
      scheduledFor: ride.scheduledFor ? new Date(ride.scheduledFor) : null,
      origin: waypoints[0]?.address ?? 'Unknown pickup',
      destination: waypoints[waypoints.length - 1]?.address ?? 'Unknown destination',
      rideId: ride.id,
    };
  });

  panicRideInfo = computed<PanicRideInfo | null>(() => {
    const ride = this.activeRide();
    if (!ride) return null;
    return {
      driverName: `${ride.driver.firstName} ${ride.driver.lastName}`,
      driverPhone: ride.driver.phone,
      vehicleModel: ride.vehicle.model,
      licensePlate: ride.vehicle.licensePlate,
      currentLocation: ride.startAddress,
      destination: ride.destinationAddress,
    };
  });

  stopRideInfo = computed<StopRideInfo | null>(() => {
    const ride = this.activeRide();
    if (!ride) return null;
    return {
      passengerName: 'Jovana Dimitrijević', // Mock data - in real app get from passenger service
      passengerRating: 4.9,
      ridesDone: 127,
      pickupLocation: ride.startAddress,
      destinationLocation: ride.destinationAddress,
      currentLocation: 'Bulevar Kralja Petra I 45, Novi Sad', // Mock data - would be actual current location
      duration: '8 min',
      distance: '2.1 km',
      completionPercent: 65,
      originalFare: 450,
      adjustedFare: 320,
      currency: 'RSD',
    };
  });

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
    const timeoutId = setTimeout(() => {
      if (this.mapComponent && this.currentLocation()) {
        this.updateMapView();
      }
      // Pass active ride to inconsistency report component
      if (this.inconsistencyReportComponent && this.activeRide()) {
        this.inconsistencyReportComponent.setActiveRide(this.activeRide());
      }
    }, 500);
    this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
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
          if (!ride) {
            console.error('Received null ride data');
            return;
          }
          this.activeRide.set(ride);
          this.currentLocation.set(ride.currentLocation);
          this.etaSeconds.set(ride.estimatedArrivalTime);
          this.requestRouteFromCurrent(ride.currentLocation, ride.destinationLocation, true, ride);
          this.updateMarkers();
          // Pass active ride to inconsistency report component
          if (this.inconsistencyReportComponent) {
            this.inconsistencyReportComponent.setActiveRide(ride);
          }
          this.startLocationUpdates(rideId);
        },
        error: (error) => {
          console.error('Error loading active ride:', error);
        },
      });
  }

  private calculateRouteCoordinates(ride: ActiveRide): void {
    if (!ride) {
      console.warn('Cannot calculate route coordinates: ride is null');
      return;
    }

    if (ride.route && ride.route.length > 0) {
      // Use waypoints from route
      const coordinates: [number, number][] = [...ride.route]
        .sort((a, b) => a.order - b.order)
        .map((waypoint) => [waypoint.lng, waypoint.lat] as [number, number]);
      this.routeCoordinates.set(coordinates);
    } else {
      // Fallback to start and destination
      const coordinates: [number, number][] = [
        [ride.startLocation.lng, ride.startLocation.lat],
        [ride.destinationLocation.lng, ride.destinationLocation.lat],
      ];
      this.routeCoordinates.set(coordinates);
    }
  }

  private requestRouteFromCurrent(
    currentLocation: { lat: number; lng: number },
    destination: { lat: number; lng: number },
    force = false,
    fallbackRide?: ActiveRide,
  ): void {
    const now = Date.now();
    if (this.routeRequestInFlight) {
      return;
    }
    if (!force && now - this.lastRouteRequestAt < this.minRerouteIntervalMs) {
      return;
    }

    this.routeRequestInFlight = true;
    this.lastRouteRequestAt = now;

    this.directionsService
      .getRoute(currentLocation, destination)
      .pipe(
        finalize(() => {
          this.routeRequestInFlight = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (coordinates) => {
          if (coordinates && coordinates.length > 1) {
            this.routeCoordinates.set(coordinates);
            this.updateMarkers();
            this.updateMapView();
          }
        },
        error: (error) => {
          console.warn('Failed to fetch road-aligned route:', error);
          const ride = fallbackRide ?? this.activeRide();
          if (ride && (!this.routeCoordinates() || this.routeCoordinates()!.length < 2)) {
            this.calculateRouteCoordinates(ride);
          }
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
          if (update.bearing !== undefined) {
            this.carBearing.set(update.bearing);
          }
          const ride = this.activeRide();
          if (ride) {
            this.requestRouteFromCurrent(
              { lat: update.lat, lng: update.lng },
              ride.destinationLocation,
            );
          }
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
    const displayLoc = this.getDisplayLocation();

    if (!ride || !displayLoc) return;

    const markers: MapMarker[] = [];

    // Only show the tracked vehicle (car icon will be used)
    markers.push({
      lat: displayLoc.lat,
      lng: displayLoc.lng,
      status: 'busy',
      driverName: `${ride.driver.firstName} ${ride.driver.lastName}`,
    });

    // Note: Start and destination are shown via route line, not markers
    // This keeps the map clean and focused on the vehicle

    this.markers.set(markers);
  }

  private updateMapView(): void {
    const displayLoc = this.getDisplayLocation();
    if (displayLoc && this.mapComponent?.getMap()) {
      this.mapComponent.setView(displayLoc.lat, displayLoc.lng);
    }
  }

  private getDisplayLocation(): { lat: number; lng: number } | null {
    const currentLoc = this.currentLocation();
    if (!currentLoc) {
      return null;
    }

    const route = this.routeCoordinates();
    if (!route || route.length < 2) {
      return currentLoc;
    }

    return this.snapToRoute(currentLoc, route);
  }

  private snapToRoute(
    point: { lat: number; lng: number },
    route: [number, number][],
  ): { lat: number; lng: number } {
    const earthRadius = 6371000;
    const refLatRad = this.toRad(point.lat);
    const cosLat = Math.cos(refLatRad) || 0.000001;

    const toXY = (lng: number, lat: number) => {
      const x = this.toRad(lng) * earthRadius * cosLat;
      const y = this.toRad(lat) * earthRadius;
      return { x, y };
    };

    const toLngLat = (x: number, y: number) => {
      const lat = this.toDeg(y / earthRadius);
      const lng = this.toDeg(x / (earthRadius * cosLat));
      return { lat, lng };
    };

    const p = toXY(point.lng, point.lat);
    let closest = { x: p.x, y: p.y };
    let minDistSq = Number.POSITIVE_INFINITY;

    for (let i = 0; i < route.length - 1; i++) {
      const [lngA, latA] = route[i];
      const [lngB, latB] = route[i + 1];
      const a = toXY(lngA, latA);
      const b = toXY(lngB, latB);
      const abx = b.x - a.x;
      const aby = b.y - a.y;
      const apx = p.x - a.x;
      const apy = p.y - a.y;
      const abLenSq = abx * abx + aby * aby;
      const t = abLenSq === 0 ? 0 : (apx * abx + apy * aby) / abLenSq;
      const clampedT = Math.max(0, Math.min(1, t));
      const proj = { x: a.x + abx * clampedT, y: a.y + aby * clampedT };
      const dx = p.x - proj.x;
      const dy = p.y - proj.y;
      const distSq = dx * dx + dy * dy;

      if (distSq < minDistSq) {
        minDistSq = distSq;
        closest = proj;
      }
    }

    return toLngLat(closest.x, closest.y);
  }

  private toRad(value: number): number {
    return (value * Math.PI) / 180;
  }

  private toDeg(value: number): number {
    return (value * 180) / Math.PI;
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

  formatScheduledTime(date: Date | null): string {
    if (!date) return 'Scheduled time TBD';
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }

  openInconsistencyForm(): void {
    this.inconsistencyReportComponent?.openModal();
  }

  closeInconsistencyForm(): void {
    this.inconsistencyReportComponent?.closeModal();
  }

  submitInconsistencyReport(): void {
    this.inconsistencyReportComponent?.submitReport();
  }

  // Panic button methods
  openPanicModal(): void {
    this.showPanicModal.set(true);
  }

  closePanicModal(): void {
    this.showPanicModal.set(false);
  }

  onPanicActivated(): void {
    console.log('PANIC ACTIVATED - sending event to backend');
    const rideId = Number(this.rideId());
    if (!rideId) return;

    this.rideApiService.createPanic(rideId).subscribe({
      next: () => {
        console.log('Panic event created for ride:', rideId);
        this.closePanicModal();
        this.panicComponent?.resetPanic();
      },
      error: (error) => {
        console.error('Failed to create panic event:', error);
        //TODO: add error message
      },
    });
  }

  onContactSupport(): void {
    console.log('Contact support clicked');
    // In real app: Open support chat or initiate call
  }

  onCancelRideFromPanic(): void {
    this.closePanicModal();
    // In real app: Navigate to cancel ride flow or open cancel dialog
    console.log('Cancel ride from panic modal');
  }

  // Stop ride button methods
  openStopModal(): void {
    this.showStopModal.set(true);
  }

  closeStopModal(): void {
    this.showStopModal.set(false);
  }

  onStopRide(): void {
    const rideId = Number(this.rideId());
    if (!rideId) {
      console.error('Ride id is missing or invalid');
      return;
    }
    this.endRideLoading.set(true);
    this.endRideError.set(null);
    this.rideApiService
      .endRide(rideId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.endRideLoading.set(false)),
      )
      .subscribe({
        next: () => {
          this.rideCompleted.set(true);
          this.closeStopModal();
          const driverId = this.activeRide()?.driver.id;
          if (driverId) {
            this.loadUpcomingRides(driverId);
          }
          // Note: Rating is handled by the passenger via ride history or notification.
          // Driver does not rate - they just see next scheduled rides.
        },
        error: (error) => {
          console.error('Failed to end ride', error);
          this.endRideError.set('Failed to end ride. Please try again.');
        },
      });
  }

  onContinueRide(): void {
    console.log('Continuing ride');
    this.closeStopModal();
  }

  navigateToUpcomingRides(): void {
    this.router.navigate(['/driver/ride-history'], { queryParams: { view: 'upcoming' } });
  }

  navigateToNextRide(): void {
    const nextRide = this.nextScheduledRide();
    if (!nextRide) {
      return;
    }
    this.router.navigate(['/ride-tracking', nextRide.id]);
  }

  private loadUpcomingRides(driverId: number): void {
    this.rideApiService
      .getUpcomingDriverRides(driverId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rides) => this.upcomingRides.set(rides),
        error: (error) => {
          console.error('Failed to load upcoming rides', error);
          this.upcomingRides.set([]);
        },
      });
  }
}
