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
import { RideTrackingApiService } from './services/ride-tracking-api.service';
import { MapboxDirectionsService } from './services/mapbox-directions.service';
import { ActiveRide, LocationUpdate } from './models/active-ride.model';
import { PanicComponent, PanicRideInfo } from './components/shared/panic/panic.component';
import { StopRideComponent, StopRideInfo } from './components/driver/stop-ride/stop-ride.component';
import { InconsistencyReportComponent } from './components/passenger/inconsistency-report/inconsistency-report.component';
import { RideApiService } from './services/ride-api.service';
import { RideResponseDto } from '../ride-history/models/ride-api.model';
import { AuthService } from '../../shared/services/auth.service';
import { ToastService } from '../../shared/services/toast.service';
import {
  buildRouteRequestPointsFromLastPassed,
  buildRouteRequestThroughRemainingWaypoints,
} from './utils/remaining-waypoints.util';

interface RouteCheckpoint {
  id: string;
  type: 'start' | 'waypoint' | 'destination';
  title: string;
  address: string;
  passed: boolean;
}

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
  private rideTrackingService = inject(RideTrackingApiService);
  private directionsService = inject(MapboxDirectionsService);
  private rideApiService = inject(RideApiService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private readonly STATUS_ACTIVE = 'ACTIVE';
  private readonly STATUS_ACCEPTED = 'ACCEPTED';
  private readonly STATUS_PENDING = 'PENDING';
  private readonly STATUS_FINISHED = 'FINISHED';
  private readonly destinationReachedRadiusMeters = 40;

  private lastRouteRequestAt = 0;
  private routeRequestInFlight = false;
  private pendingForcedReroute = false;
  private readonly minRerouteIntervalMs = 20000;
  private lastPassedWaypointOrder = signal<number>(Number.NEGATIVE_INFINITY);
  private lastBackendLocation: { lat: number; lng: number } | null = null;
  private routeProgressMeters = 0;
  private routeCumulativeDistancesMeters: number[] = [];
  private readonly minForwardMoveMeters = 8;
  private readonly maxForwardMoveMeters = 90;
  private readonly maxProjectionCatchupMeters = 120;
  private readonly defaultForwardMoveMeters = 20;
  private readonly earthRadiusMeters = 6371000;

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
  startLoading = signal<boolean>(false);
  stopLoading = signal<boolean>(false);
  stopError = signal<string | null>(null);
  upcomingRides = signal<RideResponseDto[]>([]);
  isDriverUser = computed(() => this.authService.isDriver());
  isPassengerUser = computed(() => this.authService.isPassenger());
  isRideActive = computed(() => this.activeRide()?.status === this.STATUS_ACTIVE);
  canUsePanic = computed(() => this.isRideActive());
  canReportInconsistency = computed(() => this.isPassengerUser() && this.isRideActive());
  routeCheckpoints = computed<RouteCheckpoint[]>(() => {
    const ride = this.activeRide();
    const route = ride?.route ? [...ride.route].sort((a, b) => a.order - b.order) : [];
    if (!ride || route.length === 0) {
      return [];
    }

    const lastPassedOrder = this.lastPassedWaypointOrder();
    const currentLoc = this.currentLocation();
    const destination = route[route.length - 1];
    const destinationPassed =
      ride.status === this.STATUS_FINISHED ||
      (currentLoc
        ? this.distanceMeters(currentLoc, {
            lat: destination.lat,
            lng: destination.lng,
          }) <= this.destinationReachedRadiusMeters
        : false);

    return route.map((point, index) => {
      const isStart = index === 0;
      const isDestination = index === route.length - 1;
      const passed = isDestination ? destinationPassed : point.order <= lastPassedOrder;
      return {
        id: `${point.order}-${index}`,
        type: isStart ? 'start' : isDestination ? 'destination' : 'waypoint',
        title: isStart ? 'Pickup' : isDestination ? 'Destination' : `Checkpoint ${index}`,
        address:
          point.address ??
          (isStart
            ? ride.startAddress
            : isDestination
              ? ride.destinationAddress
              : `Waypoint ${index}`),
        passed,
      };
    });
  });
  rideNotStartedMessage = computed(() => {
    if (!this.isPassengerUser()) return null;
    const status = this.activeRide()?.status;
    if (!status || status === this.STATUS_ACTIVE) return null;
    if (status === this.STATUS_ACCEPTED) {
      return 'Your ride has been accepted. Waiting for the driver to start the ride.';
    }
    if (status === this.STATUS_PENDING) {
      return 'Your ride request is pending driver assignment.';
    }
    return `Your ride has not started yet (current status: ${status}).`;
  });
  canStartRide = computed(
    () => this.isDriverUser() && this.activeRide()?.status === this.STATUS_ACCEPTED,
  );
  canStopRide = computed(
    () => this.isDriverUser() && this.activeRide()?.status === this.STATUS_ACTIVE,
  );

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
    const current = this.currentLocation();
    const currentLocationLabel = current
      ? `${current.lat.toFixed(5)}, ${current.lng.toFixed(5)}`
      : 'Current location unavailable';
    return {
      passengerName: 'Jovana Dimitrijević', // Mock data - in real app get from passenger service
      passengerRating: 4.9,
      ridesDone: 127,
      pickupLocation: ride.startAddress,
      destinationLocation: ride.destinationAddress,
      currentLocation: currentLocationLabel,
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
          if (!this.isTrackableStatus(ride.status)) {
            this.router.navigate(['/ride-tracking'], { replaceUrl: true });
            return;
          }
          this.activeRide.set(ride);
          this.lastPassedWaypointOrder.set(Number.NEGATIVE_INFINITY);
          this.currentLocation.set(ride.currentLocation);
          this.lastBackendLocation = ride.currentLocation;
          this.etaSeconds.set(ride.estimatedArrivalTime);
          this.requestRouteFromCurrent(ride.currentLocation, true, ride);
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

    const waypoints = ride.route && ride.route.length > 0
      ? [...ride.route].sort((a, b) => a.order - b.order)
      : null;

    if (waypoints && waypoints.length >= 2) {
      const coords = waypoints.map((wp) => ({ lat: wp.lat, lng: wp.lng }));
      const fallback = waypoints.map((wp) => [wp.lng, wp.lat] as [number, number]);
      this.directionsService
        .getRouteWithWaypoints(coords)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (coordinates) => {
            if (coordinates.length >= 2) {
              this.setRouteCoordinatesAndProgress(coordinates);
            }
          },
          error: () => {
            this.setRouteCoordinatesAndProgress(fallback);
          },
        });
    } else {
      const coordinates: [number, number][] = [
        [ride.startLocation.lng, ride.startLocation.lat],
        [ride.destinationLocation.lng, ride.destinationLocation.lat],
      ];
      this.setRouteCoordinatesAndProgress(coordinates);
    }
  }

  private requestRouteFromCurrent(
    currentLocation: { lat: number; lng: number },
    force = false,
    fallbackRide?: ActiveRide,
    waypointDetectionLocation?: { lat: number; lng: number },
  ): void {
    const ride = fallbackRide ?? this.activeRide();
    if (!ride) {
      return;
    }

    const routeWaypoints = ride.route && ride.route.length > 0
      ? [...ride.route].sort((a, b) => a.order - b.order)
      : [];
    const detectionLocation = waypointDetectionLocation ?? currentLocation;
    const previousLastPassedWaypointOrder = this.lastPassedWaypointOrder();
    const detectionResult = buildRouteRequestThroughRemainingWaypoints(
      detectionLocation,
      routeWaypoints,
      this.lastPassedWaypointOrder(),
    );
    this.lastPassedWaypointOrder.set(detectionResult.lastPassedWaypointOrder);
    const waypointTransitioned =
      this.lastPassedWaypointOrder() > previousLastPassedWaypointOrder;

    const now = Date.now();
    if (this.routeRequestInFlight) {
      if (waypointTransitioned) {
        this.pendingForcedReroute = true;
      }
      return;
    }
    if (
      !force &&
      !waypointTransitioned &&
      now - this.lastRouteRequestAt < this.minRerouteIntervalMs
    ) {
      return;
    }

    const routeOrigin = this.shouldUsePickupAsRouteOrigin(routeWaypoints)
      ? { lat: routeWaypoints[0].lat, lng: routeWaypoints[0].lng }
      : currentLocation;
    const routeRequestPoints = buildRouteRequestPointsFromLastPassed(
      routeOrigin,
      routeWaypoints,
      this.lastPassedWaypointOrder(),
    );

    if (routeRequestPoints.length < 2) {
      this.calculateRouteCoordinates(ride);
      return;
    }

    this.routeRequestInFlight = true;
    this.lastRouteRequestAt = now;

    this.directionsService
      .getRouteWithWaypoints(routeRequestPoints)
      .pipe(
        finalize(() => {
          this.routeRequestInFlight = false;
          if (this.pendingForcedReroute) {
            this.pendingForcedReroute = false;
            const rideForReroute = this.activeRide();
            const locForReroute = this.currentLocation();
            if (rideForReroute && locForReroute) {
              this.requestRouteFromCurrent(locForReroute, true, rideForReroute, locForReroute);
            }
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (coordinates) => {
          if (coordinates && coordinates.length > 1) {
            this.setRouteCoordinatesAndProgress(coordinates);
            this.updateMarkers();
            this.updateMapView();
          }
        },
        error: (error) => {
          console.warn('Failed to fetch road-aligned route:', error);
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
          const backendLocation = { lat: update.lat, lng: update.lng };
          const displayLocation = this.computeNextLocationOnRoute(backendLocation);
          this.currentLocation.set(displayLocation);
          this.lastBackendLocation = backendLocation;
          this.etaSeconds.set(update.estimatedArrivalTime);
          if (update.bearing !== undefined) {
            this.carBearing.set(update.bearing);
          }
          const ride = this.activeRide();
          if (ride) {
            this.requestRouteFromCurrent(displayLocation, false, ride, backendLocation);
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
    const routeWaypoints = ride.route ? [...ride.route].sort((a, b) => a.order - b.order) : [];

    if (routeWaypoints.length >= 2) {
      const start = routeWaypoints[0];
      const destination = routeWaypoints[routeWaypoints.length - 1];
      const middleWaypoints = routeWaypoints.slice(1, -1);

      markers.push({
        lat: start.lat,
        lng: start.lng,
        status: 'available',
        kind: 'start',
        label: 'Start',
      });

      middleWaypoints.forEach((waypoint, index) => {
        markers.push({
          lat: waypoint.lat,
          lng: waypoint.lng,
          status: 'available',
          kind: 'waypoint',
          label: `Waypoint ${index + 1}`,
        });
      });

      markers.push({
        lat: destination.lat,
        lng: destination.lng,
        status: 'available',
        kind: 'destination',
        label: 'Destination',
      });
    }

    // Only show the tracked vehicle (car icon will be used)
    markers.push({
      lat: displayLoc.lat,
      lng: displayLoc.lng,
      status: 'busy',
      kind: 'vehicle',
      driverName: `${ride.driver.firstName} ${ride.driver.lastName}`,
    });

    this.markers.set(markers);
  }

  private updateMapView(): void {
    const displayLoc = this.getDisplayLocation();
    if (displayLoc && this.mapComponent?.getMap()) {
      this.mapComponent.setView(displayLoc.lat, displayLoc.lng);
    }
  }

  private getDisplayLocation(): { lat: number; lng: number } | null {
    return this.currentLocation();
  }

  private shouldUsePickupAsRouteOrigin(
    routeWaypoints: Array<{ lat: number; lng: number; order: number }>,
  ): boolean {
    return (
      this.lastPassedWaypointOrder() === Number.NEGATIVE_INFINITY &&
      routeWaypoints.length >= 2
    );
  }

  private setRouteCoordinatesAndProgress(coordinates: [number, number][]): void {
    this.routeCoordinates.set(coordinates);
    this.routeCumulativeDistancesMeters = this.buildRouteCumulativeDistances(coordinates);
    if (this.routeCumulativeDistancesMeters.length < 2) {
      this.routeProgressMeters = 0;
      return;
    }

    const current = this.currentLocation();
    if (!current) {
      this.routeProgressMeters = 0;
      return;
    }

    this.routeProgressMeters = this.closestDistanceAlongRoute(
      current,
      coordinates,
      this.routeCumulativeDistancesMeters,
    );
    const snapped = this.pointAlongRouteAtDistance(
      coordinates,
      this.routeCumulativeDistancesMeters,
      this.routeProgressMeters,
    );
    this.currentLocation.set(snapped);
  }

  private computeNextLocationOnRoute(backendLocation: { lat: number; lng: number }): {
    lat: number;
    lng: number;
  } {
    const route = this.routeCoordinates();
    if (!route || route.length < 2) {
      return backendLocation;
    }

    if (this.routeCumulativeDistancesMeters.length !== route.length) {
      this.routeCumulativeDistancesMeters = this.buildRouteCumulativeDistances(route);
    }
    if (this.routeCumulativeDistancesMeters.length < 2) {
      return backendLocation;
    }

    const backendProjectedDistance = this.closestDistanceAlongRoute(
      backendLocation,
      route,
      this.routeCumulativeDistancesMeters,
    );
    this.routeProgressMeters = Math.max(this.routeProgressMeters, backendProjectedDistance);

    let forwardMoveMeters = this.defaultForwardMoveMeters;
    if (this.lastBackendLocation) {
      forwardMoveMeters = this.distanceMeters(this.lastBackendLocation, backendLocation);
    }
    forwardMoveMeters = Math.max(
      this.minForwardMoveMeters,
      Math.min(this.maxForwardMoveMeters, forwardMoveMeters),
    );

    const totalRouteDistance =
      this.routeCumulativeDistancesMeters[this.routeCumulativeDistancesMeters.length - 1] ?? 0;
    const projectedAheadDistance = backendProjectedDistance - this.routeProgressMeters;
    const canCatchUpToProjection =
      projectedAheadDistance > 0 &&
      projectedAheadDistance <= this.maxProjectionCatchupMeters;
    const projectedTarget = canCatchUpToProjection
      ? backendProjectedDistance
      : this.routeProgressMeters;
    const stepTarget = this.routeProgressMeters + forwardMoveMeters;
    this.routeProgressMeters = Math.min(
      totalRouteDistance,
      Math.max(stepTarget, projectedTarget),
    );

    return this.pointAlongRouteAtDistance(
      route,
      this.routeCumulativeDistancesMeters,
      this.routeProgressMeters,
    );
  }

  private buildRouteCumulativeDistances(route: [number, number][]): number[] {
    if (!route || route.length < 2) {
      return [];
    }
    const cumulativeDistances: number[] = [0];
    for (let i = 1; i < route.length; i++) {
      const previous = { lat: route[i - 1][1], lng: route[i - 1][0] };
      const current = { lat: route[i][1], lng: route[i][0] };
      cumulativeDistances.push(
        cumulativeDistances[i - 1] + this.distanceMeters(previous, current),
      );
    }
    return cumulativeDistances;
  }

  private closestDistanceAlongRoute(
    point: { lat: number; lng: number },
    route: [number, number][],
    cumulativeDistances: number[],
  ): number {
    if (route.length < 2 || cumulativeDistances.length < 2) {
      return 0;
    }

    const refLatRad = this.toRad(point.lat);
    const cosLat = Math.cos(refLatRad) || 0.000001;

    const toXY = (lng: number, lat: number) => {
      const x = this.toRad(lng) * this.earthRadiusMeters * cosLat;
      const y = this.toRad(lat) * this.earthRadiusMeters;
      return { x, y };
    };

    const projectedPoint = toXY(point.lng, point.lat);
    let minDistSq = Number.POSITIVE_INFINITY;
    let closestDistanceAlongRoute = 0;

    for (let i = 0; i < route.length - 1; i++) {
      const [lngA, latA] = route[i];
      const [lngB, latB] = route[i + 1];
      const a = toXY(lngA, latA);
      const b = toXY(lngB, latB);
      const abx = b.x - a.x;
      const aby = b.y - a.y;
      const apx = projectedPoint.x - a.x;
      const apy = projectedPoint.y - a.y;
      const abLenSq = abx * abx + aby * aby;
      const t = abLenSq <= 0 ? 0 : (apx * abx + apy * aby) / abLenSq;
      const clampedT = Math.max(0, Math.min(1, t));
      const projX = a.x + abx * clampedT;
      const projY = a.y + aby * clampedT;
      const dx = projectedPoint.x - projX;
      const dy = projectedPoint.y - projY;
      const distSq = dx * dx + dy * dy;

      if (distSq < minDistSq) {
        minDistSq = distSq;
        const segmentStart = cumulativeDistances[i] ?? 0;
        const segmentEnd = cumulativeDistances[i + 1] ?? segmentStart;
        closestDistanceAlongRoute = segmentStart + (segmentEnd - segmentStart) * clampedT;
      }
    }

    return closestDistanceAlongRoute;
  }

  private pointAlongRouteAtDistance(
    route: [number, number][],
    cumulativeDistances: number[],
    distanceMeters: number,
  ): { lat: number; lng: number } {
    if (!route.length) {
      return { lat: 0, lng: 0 };
    }
    if (route.length === 1 || cumulativeDistances.length < 2) {
      return { lat: route[0][1], lng: route[0][0] };
    }

    if (distanceMeters <= 0) {
      return { lat: route[0][1], lng: route[0][0] };
    }

    const totalDistance = cumulativeDistances[cumulativeDistances.length - 1] ?? 0;
    if (distanceMeters >= totalDistance) {
      const last = route[route.length - 1];
      return { lat: last[1], lng: last[0] };
    }

    for (let i = 1; i < cumulativeDistances.length; i++) {
      const segmentEndDistance = cumulativeDistances[i];
      if (distanceMeters > segmentEndDistance) {
        continue;
      }
      const segmentStartDistance = cumulativeDistances[i - 1];
      const segmentLength = segmentEndDistance - segmentStartDistance;
      const ratio =
        segmentLength <= 0 ? 0 : (distanceMeters - segmentStartDistance) / segmentLength;
      const [startLng, startLat] = route[i - 1];
      const [endLng, endLat] = route[i];
      return {
        lat: startLat + (endLat - startLat) * ratio,
        lng: startLng + (endLng - startLng) * ratio,
      };
    }

    const fallback = route[route.length - 1];
    return { lat: fallback[1], lng: fallback[0] };
  }

  private distanceMeters(
    from: { lat: number; lng: number },
    to: { lat: number; lng: number },
  ): number {
    const lat1 = this.toRad(from.lat);
    const lat2 = this.toRad(to.lat);
    const dLat = lat2 - lat1;
    const dLng = this.toRad(to.lng - from.lng);
    const sinLat = Math.sin(dLat / 2);
    const sinLng = Math.sin(dLng / 2);
    const a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return this.earthRadiusMeters * c;
  }

  private toRad(value: number): number {
    return (value * Math.PI) / 180;
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

  private isTrackableStatus(status: string | null | undefined): boolean {
    return (
      status === this.STATUS_PENDING ||
      status === this.STATUS_ACCEPTED ||
      status === this.STATUS_ACTIVE
    );
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
    if (!this.canUsePanic()) {
      const currentStatus = this.activeRide()?.status ?? 'UNKNOWN';
      this.toastService.warning(
        `Panic is available only during an active ride. Current status: ${currentStatus}.`,
      );
      return;
    }
    this.showPanicModal.set(true);
  }

  closePanicModal(): void {
    this.showPanicModal.set(false);
  }

  onPanicActivated(): void {
    console.log('PANIC ACTIVATED - sending event to backend');
    const rideId = Number(this.rideId());
    if (!rideId) {
      console.error('Ride id is missing or invalid, cannot activate panic');
      this.closePanicModal();
      this.panicComponent?.resetPanic();
      return;
    }

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
    if (!this.isDriverUser()) {
      return;
    }
    const ride = this.activeRide();
    if (!ride || ride.status !== this.STATUS_ACTIVE) {
      const currentStatus = ride?.status ?? 'UNKNOWN';
      const message = currentStatus === this.STATUS_ACCEPTED
        ? 'Ride is ACCEPTED. Start the ride first, then you can stop it.'
        : `Ride can be stopped only when ACTIVE. Current status: ${currentStatus}.`;
      this.toastService.warning(message);
      return;
    }
    this.showStopModal.set(true);
  }

  onStartRide(): void {
    if (!this.isDriverUser()) {
      return;
    }
    const rideId = Number(this.rideId());
    if (!rideId) {
      this.toastService.error('Ride id is missing or invalid.');
      return;
    }
    const ride = this.activeRide();
    if (!ride || ride.status !== this.STATUS_ACCEPTED) {
      const currentStatus = ride?.status ?? 'UNKNOWN';
      this.toastService.warning(
        `Ride can be started only when ACCEPTED. Current status: ${currentStatus}.`,
      );
      return;
    }

    this.startLoading.set(true);
    this.rideApiService
      .startRide(rideId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.startLoading.set(false)),
      )
      .subscribe({
        next: () => {
          this.toastService.success('Ride started successfully.');
          this.loadActiveRide(String(rideId));
        },
        error: (error) => {
          const message = error?.error?.message || 'Failed to start ride. Please try again.';
          this.toastService.error(message);
        },
      });
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

  onConfirmStop(stopAddress: string): void {
    const rideId = Number(this.rideId());
    if (!rideId) {
      console.error('Ride id is missing or invalid, cannot stop ride');
      return;
    }

    const loc = this.currentLocation();
    if (!loc) {
      console.error('Current location missing, cannot send stop coordinates');
      this.stopError.set('Current location unknown');
      return;
    }
    const ride = this.activeRide();
    if (!ride || ride.status !== this.STATUS_ACTIVE) {
      const currentStatus = ride?.status ?? 'UNKNOWN';
      this.toastService.warning(
        `Ride can be stopped only when ACTIVE. Current status: ${currentStatus}.`,
      );
      return;
    }

    this.stopLoading.set(true);
    this.stopError.set(null);

    const request = {
      stopAddress,
      stopLat: loc.lat,
      stopLng: loc.lng,
    };

    this.rideApiService
      .stopRide(rideId, request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.stopLoading.set(false)),
      )
      .subscribe({
        next: (response) => {
          console.log('Ride stopped successfully', response);
          // Normalize API response to ActiveRide shape (RideResponseDto is not ActiveRide-compatible).
          const orderedWaypoints = [...(response.waypoints ?? [])].sort((a, b) => a.order - b.order);
          const finalWaypoint = orderedWaypoints[orderedWaypoints.length - 1];
          const nextLat = finalWaypoint?.lat ?? loc.lat;
          const nextLng = finalWaypoint?.lng ?? loc.lng;
          const resolvedStopAddress = finalWaypoint?.address ?? stopAddress;
          const current = this.activeRide();
          const normalizedRide: ActiveRide = current
            ? {
                ...current,
                id: String(response.id),
                status: response.status,
                currentLocation: { lat: nextLat, lng: nextLng },
                destinationLocation: { lat: nextLat, lng: nextLng },
                destinationAddress: resolvedStopAddress,
                estimatedArrivalTime: 0,
                route: [],
              }
            : {
                id: String(response.id),
                status: response.status,
                startAddress: resolvedStopAddress,
                destinationAddress: resolvedStopAddress,
                startLocation: { lat: nextLat, lng: nextLng },
                destinationLocation: { lat: nextLat, lng: nextLng },
                currentLocation: { lat: nextLat, lng: nextLng },
                driver: {
                  id: response.driverId ?? 0,
                  firstName: response.driverName ?? 'Driver',
                  lastName: response.driverSurname ?? '',
                  phone: '',
                },
                vehicle: {
                  id: response.vehicleId ?? 0,
                  model: '',
                  licensePlate: '',
                  vehicleType: 'STANDARD',
                },
                estimatedArrivalTime: 0,
                startTime: response.startTime ? new Date(response.startTime) : new Date(),
                route: [],
              };
          this.activeRide.set(normalizedRide);
          this.currentLocation.set({ lat: nextLat, lng: nextLng });
          this.etaSeconds.set(0);
          this.routeCoordinates.set(undefined);
          this.carBearing.set(undefined);
          this.closeStopModal();
          this.toastService.success('Ride stopped successfully.');
          this.router.navigate(['/driver/ride-history'], { replaceUrl: true });
        },
        error: (error) => {
          console.error('Failed to stop ride', error);
          const message = error?.error?.message || 'Failed to stop ride. Please try again.';
          this.stopError.set(message);
          this.toastService.error(message);
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
