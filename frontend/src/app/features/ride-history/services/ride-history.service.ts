import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Ride } from '../models';
import {
  DriverRideHistoryItemDto,
  PageResponse,
  RideResponseDto,
} from '../models/ride-api.model';

/**
 * Paginated response for driver ride history.
 */
export interface DriverRideHistoryResponse {
  rides: Ride[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

/**
 * Multi-role ride history service.
 * Handles data fetching for passenger, driver, and admin roles.
 * Business logic for all roles is centralized here.
 */
@Injectable({ providedIn: 'root' })
export class RideHistoryService {
  private readonly http = inject(HttpClient);

  /**
   * Get ride history for a passenger.
   * Returns rides where the user was a passenger.
   */
  getPassengerRideHistory(): Ride[] {
    return this.getMockRideData();
  }

  /**
   * Get ride history for a driver with server-side pagination and filtering.
   * Calls GET /api/drivers/{driverId}/rides/history
   */
  getDriverRideHistory(
    driverId: number,
    from?: Date | null,
    to?: Date | null,
    page = 0,
    size = 10,
    sort = 'requestedAt,desc',
  ): Observable<DriverRideHistoryResponse> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);

    if (from) {
      params = params.set('from', from.toISOString());
    }
    if (to) {
      // Set end of day for the 'to' date
      const endOfDay = new Date(to);
      endOfDay.setHours(23, 59, 59, 999);
      params = params.set('to', endOfDay.toISOString());
    }

    return this.http
      .get<PageResponse<DriverRideHistoryItemDto>>(
        `${environment.apiBaseUrl}/drivers/${driverId}/rides/history`,
        { params },
      )
      .pipe(
        map((response) => ({
          rides: response.content.map((item) => this.mapDriverHistoryItem(item)),
          totalElements: response.totalElements,
          totalPages: response.totalPages,
          currentPage: response.number,
          pageSize: response.size,
        })),
      );
  }

  getUpcomingDriverRides(driverId: number, page = 0, size = 10): Observable<Ride[]> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'scheduledFor,asc');

    return this.http
      .get<PageResponse<RideResponseDto>>(
        `${environment.apiBaseUrl}/drivers/${driverId}/rides/upcoming`,
        { params },
      )
      .pipe(map((response) => response.content.map((ride) => this.mapRideResponse(ride))));
  }

  /**
   * Get ride history for an admin.
   * Returns all rides in the system (with admin visibility).
   */
  getAdminRideHistory(): Ride[] {
    return this.getMockRideData();
  }

  /**
   * Get a single ride by ID (with role context).
   */
  getRideById(rideId: string): Ride | undefined {
    return this.getMockRideData().find((r) => r.id === rideId);
  }

  /**
   * Filter rides by date range (applicable to all roles).
   */
  filterRidesByDateRange(rides: Ride[], startDate: Date | null, endDate: Date | null): Ride[] {
    if (!startDate && !endDate) {
      return rides;
    }

    return rides.filter((ride) => {
      const rideDate = new Date(ride.startTime);
      rideDate.setHours(0, 0, 0, 0);

      if (startDate) {
        const start = new Date(startDate);
        start.setHours(0, 0, 0, 0);
        if (rideDate < start) return false;
      }

      if (endDate) {
        const end = new Date(endDate);
        end.setHours(23, 59, 59, 999);
        if (rideDate > end) return false;
      }

      return true;
    });
  }

  /**
   * Sort rides by field and order.
   */
  sortRides(rides: Ride[], field: string | null, order: 'asc' | 'desc'): Ride[] {
    if (!field) {
      return rides;
    }

    const sorted = [...rides].sort((a, b) => {
      let valueA: any;
      let valueB: any;

      switch (field) {
        case 'dateTime':
          valueA = new Date(a.startTime).getTime();
          valueB = new Date(b.startTime).getTime();
          break;
        case 'driver':
          valueA = `${a.driver?.firstName || ''}${a.driver?.lastName || ''}`;
          valueB = `${b.driver?.firstName || ''}${b.driver?.lastName || ''}`;
          break;
        case 'passengers':
          valueA = a.passengers.length;
          valueB = b.passengers.length;
          break;
        case 'route':
          valueA = `${a.origin}${a.destination}`;
          valueB = `${b.origin}${b.destination}`;
          break;
        case 'status':
          valueA = a.isCancelled ? 1 : 0;
          valueB = b.isCancelled ? 1 : 0;
          break;
        case 'earnings':
          valueA = a.cost;
          valueB = b.cost;
          break;
        default:
          return 0;
      }

      if (valueA < valueB) return order === 'asc' ? -1 : 1;
      if (valueA > valueB) return order === 'asc' ? 1 : -1;
      return 0;
    });

    return sorted;
  }

  /**
   * Get mock ride data.
   * IDs match the SQL test data from ride_rating.sql (8001-8006).
   * In a real app, this would be replaced with HTTP calls.
   */
  private getMockRideData(): Ride[] {
    const now = new Date();
    const day = 24 * 60 * 60 * 1000;

    return [
      // Ride 8001: Already rated (2 days ago)
      {
        id: '8001',
        startTime: new Date(now.getTime() - 2 * day),
        endTime: new Date(now.getTime() - 2 * day + 25 * 60 * 1000),
        origin: 'Bulevar Oslobodjenja 1, Novi Sad',
        destination: 'Trg Slobode 1, Novi Sad',
        cost: 450,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Rating',
            lastName: 'TestPassenger',
            email: 'rating.passenger@test.local',
            phone: '+381 64 222 0002',
          },
        ],
        driver: {
          firstName: 'Rating',
          lastName: 'TestDriver',
          email: 'rating.driver@test.local',
          phone: '+381 64 111 0001',
          rating: 4.8,
        },
        vehicleType: 'STANDARD',
        driverRating: 5,
        vehicleRating: 4,
      },
      // Ride 8002: Eligible for rating (1 day ago, ~2 days left)
      {
        id: '8002',
        startTime: new Date(now.getTime() - 1 * day),
        endTime: new Date(now.getTime() - 1 * day + 18 * 60 * 1000),
        origin: 'Petrovaradin, Novi Sad',
        destination: 'Liman, Novi Sad',
        cost: 380,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Rating',
            lastName: 'TestPassenger',
            email: 'rating.passenger@test.local',
            phone: '+381 64 222 0002',
          },
        ],
        driver: {
          firstName: 'Rating',
          lastName: 'TestDriver',
          email: 'rating.driver@test.local',
          phone: '+381 64 111 0001',
          rating: 4.8,
        },
        vehicleType: 'STANDARD',
      },
      // Ride 8003: Deadline passed (5 days ago)
      {
        id: '8003',
        startTime: new Date(now.getTime() - 5 * day),
        endTime: new Date(now.getTime() - 5 * day + 30 * 60 * 1000),
        origin: 'Detelinara, Novi Sad',
        destination: 'Telep, Novi Sad',
        cost: 520,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Rating',
            lastName: 'TestPassenger',
            email: 'rating.passenger@test.local',
            phone: '+381 64 222 0002',
          },
        ],
        driver: {
          firstName: 'Rating',
          lastName: 'TestDriver',
          email: 'rating.driver@test.local',
          phone: '+381 64 111 0001',
          rating: 4.8,
        },
        vehicleType: 'STANDARD',
      },
      // Ride 8004: At deadline boundary (~71 hours ago, barely eligible)
      {
        id: '8004',
        startTime: new Date(now.getTime() - 71 * 60 * 60 * 1000),
        endTime: new Date(now.getTime() - 71 * 60 * 60 * 1000 + 22 * 60 * 1000),
        origin: 'Podbara, Novi Sad',
        destination: 'Grbavica, Novi Sad',
        cost: 410,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Rating',
            lastName: 'TestPassenger',
            email: 'rating.passenger@test.local',
            phone: '+381 64 222 0002',
          },
        ],
        driver: {
          firstName: 'Rating',
          lastName: 'TestDriver',
          email: 'rating.driver@test.local',
          phone: '+381 64 111 0001',
          rating: 4.8,
        },
        vehicleType: 'STANDARD',
      },
      // Ride 8005: Cancelled (cannot rate)
      {
        id: '8005',
        startTime: new Date(now.getTime() - 1 * day),
        endTime: null,
        origin: 'Sajmiste, Novi Sad',
        destination: 'Novo Naselje, Novi Sad',
        cost: 0,
        isCancelled: true,
        cancelledBy: 'DRIVER',
        cancellationReason: 'Passenger was not at pickup location',
        panicActivated: false,
        passengers: [
          {
            firstName: 'Rating',
            lastName: 'TestPassenger',
            email: 'rating.passenger@test.local',
            phone: '+381 64 222 0002',
          },
        ],
        driver: {
          firstName: 'Rating',
          lastName: 'TestDriver',
          email: 'rating.driver@test.local',
          phone: '+381 64 111 0001',
          rating: 4.8,
        },
        vehicleType: 'STANDARD',
      },
      // Ride 8006: Active (not finished yet)
      {
        id: '8006',
        startTime: new Date(now.getTime()),
        endTime: null,
        origin: 'Klisa, Novi Sad',
        destination: 'Adamovicevo Naselje, Novi Sad',
        cost: 480,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        status: 'ACTIVE',
        passengers: [
          {
            firstName: 'Rating',
            lastName: 'TestPassenger',
            email: 'rating.passenger@test.local',
            phone: '+381 64 222 0002',
          },
        ],
        driver: {
          firstName: 'Rating',
          lastName: 'TestDriver',
          email: 'rating.driver@test.local',
          phone: '+381 64 111 0001',
          rating: 4.8,
        },
        vehicleType: 'STANDARD',
      },
    ];
  }

  private mapRideResponse(ride: RideResponseDto): Ride {
    const waypointAddresses = ride.waypoints?.sort((a, b) => a.order - b.order) || [];
    const origin = waypointAddresses[0]?.address ?? 'Unknown pickup';
    const destination = waypointAddresses[waypointAddresses.length - 1]?.address ?? 'Unknown destination';
    const startTime = ride.scheduledFor ?? ride.startTime ?? ride.requestedAt;

    return {
      id: String(ride.id),
      startTime: startTime ? new Date(startTime) : new Date(),
      endTime: ride.endTime ? new Date(ride.endTime) : null,
      origin,
      destination,
      cost: ride.totalCost ?? 0,
      isCancelled: ride.status === 'CANCELLED',
      cancelledBy: null,
      cancellationReason: undefined,
      panicActivated: false,
      passengers: [],
      vehicleType: undefined,
      status: ride.status,
      scheduledFor: ride.scheduledFor ? new Date(ride.scheduledFor) : null,
      requestedAt: ride.requestedAt ? new Date(ride.requestedAt) : null,
    };
  }

  /**
   * Map DriverRideHistoryItemDto from backend to frontend Ride model.
   */
  private mapDriverHistoryItem(item: DriverRideHistoryItemDto): Ride {
    // Determine who cancelled the ride
    let cancelledBy: 'DRIVER' | 'PASSENGER' | null = null;
    if (item.cancelled && item.canceledByName) {
      // If cancelled by someone, we check if it's driver or passenger
      // For now, we just set based on the fact that it's cancelled
      cancelledBy = 'DRIVER'; // Backend could provide role info for more accuracy
    }

    return {
      id: String(item.id),
      startTime: item.startTime ? new Date(item.startTime) : new Date(),
      endTime: item.endTime ? new Date(item.endTime) : null,
      origin: item.startLocation?.address ?? 'Unknown pickup',
      destination: item.endLocation?.address ?? 'Unknown destination',
      cost: item.totalCost ?? 0,
      isCancelled: item.cancelled,
      cancelledBy,
      cancellationReason: item.canceledByName
        ? `Cancelled by ${item.canceledByName} ${item.canceledBySurname ?? ''}`
        : undefined,
      panicActivated: item.panicOccurred,
      passengers: item.passengers.map((p) => ({
        firstName: p.name,
        lastName: p.surname,
        email: '', // Not provided in this DTO
        phone: '', // Not provided in this DTO
      })),
      status: item.status,
    };
  }
}
