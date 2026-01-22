import { Injectable } from '@angular/core';
import { Observable, interval, map, takeWhile } from 'rxjs';
import {
  ActiveRide,
  LocationUpdate,
  RideInconsistencyReport,
} from '../models/active-ride.model';

/**
 * Mock service for ride tracking
 * Simulates real-time vehicle location updates and ETA calculations
 */
@Injectable({ providedIn: 'root' })
export class RideTrackingMockService {
  private inconsistencyReports = new Map<string, RideInconsistencyReport[]>();

  // Mock active ride data - simulating a ride in Novi Sad
  private mockActiveRide: ActiveRide = {
    id: '1',
    startAddress: 'Trg Slobode, Novi Sad',
    destinationAddress: 'Petrovaradin Fortress, Novi Sad',
    startLocation: {
      lat: 45.2671,
      lng: 19.8335, // Trg Slobode (City center)
    },
    destinationLocation: {
      lat: 45.2517,
      lng: 19.8369, // Petrovaradin Fortress
    },
    currentLocation: {
      lat: 45.2671,
      lng: 19.8335, // Starts at start location
    },
    driver: {
      id: 101,
      firstName: 'Marko',
      lastName: 'Petrović',
      phone: '+381 64 111 2222',
    },
    vehicle: {
      id: 1,
      model: 'Toyota Corolla',
      licensePlate: 'NS-123-AB',
      vehicleType: 'STANDARD',
    },
    estimatedArrivalTime: 0,
    startTime: new Date(),
    route: [
      { lat: 45.2671, lng: 19.8335, order: 0 }, // Start
      { lat: 45.2650, lng: 19.8345, order: 1 },
      { lat: 45.2620, lng: 19.8355, order: 2 },
      { lat: 45.2590, lng: 19.8360, order: 3 },
      { lat: 45.2560, lng: 19.8365, order: 4 },
      { lat: 45.2517, lng: 19.8369, order: 5 }, // Destination
    ],
  };

  /**
   * Get active ride data
   */
  getActiveRide(rideId: string): Observable<ActiveRide> {
    // In real implementation, this would fetch from backend
    // For now, return mock data
    const ride = { ...this.mockActiveRide };
    ride.id = rideId;
    ride.estimatedArrivalTime = this.calculateETA(
      ride.currentLocation,
      ride.destinationLocation
    );
    return new Observable((observer) => {
      observer.next(ride);
      observer.complete();
    });
  }

  /**
   * Get vehicle location updates as an Observable
   * Emits updates every 2-3 seconds simulating vehicle movement
   */
  getVehicleLocationUpdates(rideId: string): Observable<LocationUpdate> {
    const ride = { ...this.mockActiveRide };
    ride.id = rideId;

    // Calculate total distance and steps
    const route = ride.route || [
      ride.startLocation,
      ride.destinationLocation,
    ];
    const totalSteps = route.length - 1;
    let currentStep = 0;
    let progress = 0; // 0 to 1
    let previousLocation: { lat: number; lng: number } | null = null;

    // Simulate movement along route
    return interval(2500).pipe(
      map(() => {
        if (currentStep >= totalSteps) {
          // Reached destination
          const destination = {
            lat: ride.destinationLocation.lat,
            lng: ride.destinationLocation.lng,
          };
          const bearing = previousLocation
            ? this.calculateBearing(previousLocation, destination)
            : 0;

          return {
            lat: destination.lat,
            lng: destination.lng,
            timestamp: new Date(),
            estimatedArrivalTime: 0,
            bearing,
          };
        }

        // Interpolate between current and next waypoint
        const currentWaypoint = route[currentStep];
        const nextWaypoint = route[currentStep + 1];

        const stepProgress = progress % 1;
        const lat =
          currentWaypoint.lat +
          (nextWaypoint.lat - currentWaypoint.lat) * stepProgress;
        const lng =
          currentWaypoint.lng +
          (nextWaypoint.lng - currentWaypoint.lng) * stepProgress;

        progress += 0.15; // Move 15% per update (adjust for speed)

        if (progress >= 1) {
          currentStep++;
          progress = 0;
        }

        const currentLocation = { lat, lng };
        const eta = this.calculateETA(
          currentLocation,
          ride.destinationLocation
        );

        // Calculate bearing from previous location
        let bearing = 0;
        if (previousLocation) {
          bearing = this.calculateBearing(previousLocation, currentLocation);
        } else {
          // For first update, calculate bearing to next waypoint
          const nextWaypoint = route[Math.min(currentStep + 1, totalSteps)];
          bearing = this.calculateBearing(currentLocation, nextWaypoint);
        }

        previousLocation = currentLocation;

        return {
          lat,
          lng,
          timestamp: new Date(),
          estimatedArrivalTime: eta,
          bearing,
        };
      }),
      takeWhile(
        (update) => update.estimatedArrivalTime > 5,
        true // inclusive - include the last update
      )
    );
  }

  /**
   * Calculate ETA in seconds based on distance and average speed
   * Uses Haversine formula for distance calculation
   */
  private calculateETA(
    currentLocation: { lat: number; lng: number },
    destination: { lat: number; lng: number }
  ): number {
    const distance = this.calculateDistance(currentLocation, destination);
    const averageSpeedKmh = 50; // 50 km/h average speed
    const averageSpeedMs = averageSpeedKmh / 3.6; // Convert to m/s
    const timeInSeconds = distance / averageSpeedMs;

    return Math.max(0, Math.round(timeInSeconds));
  }

  /**
   * Calculate distance between two points using Haversine formula
   * Returns distance in meters
   */
  private calculateDistance(
    point1: { lat: number; lng: number },
    point2: { lat: number; lng: number }
  ): number {
    const R = 6371000; // Earth radius in meters
    const dLat = this.toRad(point2.lat - point1.lat);
    const dLng = this.toRad(point2.lng - point1.lng);

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(point1.lat)) *
        Math.cos(this.toRad(point2.lat)) *
        Math.sin(dLng / 2) *
        Math.sin(dLng / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private toRad(degrees: number): number {
    return (degrees * Math.PI) / 180;
  }

  private toDeg(radians: number): number {
    return (radians * 180) / Math.PI;
  }

  /**
   * Calculate bearing (direction) between two points in degrees
   * Returns bearing from 0 to 360 degrees (0 = North, 90 = East, etc.)
   */
  private calculateBearing(
    from: { lat: number; lng: number },
    to: { lat: number; lng: number }
  ): number {
    const dLng = this.toRad(to.lng - from.lng);
    const lat1 = this.toRad(from.lat);
    const lat2 = this.toRad(to.lat);

    const y = Math.sin(dLng) * Math.cos(lat2);
    const x =
      Math.cos(lat1) * Math.sin(lat2) -
      Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

    let bearing = Math.atan2(y, x);
    bearing = this.toDeg(bearing);
    bearing = (bearing + 360) % 360; // Normalize to 0-360

    return Math.round(bearing);
  }

  /**
   * Report driver inconsistency
   */
  reportInconsistency(
    rideId: string,
    note: string,
    reportedBy: { firstName: string; lastName: string; email: string }
  ): Observable<RideInconsistencyReport> {
    const report: RideInconsistencyReport = {
      id: Date.now().toString(),
      rideId,
      note,
      reportedAt: new Date(),
      reportedBy,
    };

    if (!this.inconsistencyReports.has(rideId)) {
      this.inconsistencyReports.set(rideId, []);
    }
    this.inconsistencyReports.get(rideId)!.push(report);

    return new Observable((observer) => {
      // Simulate API delay
      setTimeout(() => {
        observer.next(report);
        observer.complete();
      }, 500);
    });
  }

  /**
   * Get inconsistency reports for a ride
   */
  getInconsistencyReports(rideId: string): RideInconsistencyReport[] {
    return this.inconsistencyReports.get(rideId) || [];
  }
}
