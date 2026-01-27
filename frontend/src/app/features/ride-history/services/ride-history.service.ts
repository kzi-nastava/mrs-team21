import { Injectable } from '@angular/core';
import { Ride } from '../models';

/**
 * Multi-role ride history service.
 * Handles data fetching for passenger, driver, and admin roles.
 * Business logic for all roles is centralized here.
 */
@Injectable({ providedIn: 'root' })
export class RideHistoryService {
  /**
   * Get ride history for a passenger.
   * Returns rides where the user was a passenger.
   */
  getPassengerRideHistory(): Ride[] {
    return this.getMockRideData();
  }

  /**
   * Get ride history for a driver.
   * Returns rides where the user was the driver.
   */
  getDriverRideHistory(): Ride[] {
    return this.getMockRideData();
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
   * In a real app, this would be replaced with HTTP calls.
   */
  private getMockRideData(): Ride[] {
    const now = new Date();

    return [
      {
        id: '1',
        startTime: new Date(now.getTime() - 2 * 60 * 60 * 1000),
        endTime: new Date(now.getTime() - 1 * 60 * 60 * 1000),
        origin: 'Bulevar Kralja Aleksandra 73, Belgrade',
        destination: 'Nikola Tesla Airport, Belgrade',
        cost: 2500,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Marko',
            lastName: 'Petrovic',
            email: 'marko.petrovic@example.com',
            phone: '+381 64 111 2222',
          },
          {
            firstName: 'Ana',
            lastName: 'Jovanovic',
            email: 'ana.jovanovic@example.com',
            phone: '+381 64 333 4444',
          },
        ],
        driver: {
          firstName: 'Dejan',
          lastName: 'Kovacevic',
          email: 'dejan.kovacevic@example.com',
          phone: '+381 64 555 1111',
          rating: 4.8,
        },
        vehicleType: 'LUXURY',
        driverRating: 4.8,
        passengerRating: 4.9,
      },
      {
        id: '2',
        startTime: new Date(now.getTime() - 5 * 60 * 60 * 1000),
        endTime: new Date(now.getTime() - 4 * 60 * 60 * 1000),
        origin: 'Trg Republike, Belgrade',
        destination: 'Ada Ciganlija, Belgrade',
        cost: 1200,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Stefan',
            lastName: 'Nikolic',
            email: 'stefan.nikolic@example.com',
            phone: '+381 64 555 6666',
          },
        ],
        driver: {
          firstName: 'Aleksandar',
          lastName: 'Milosevic',
          email: 'aleksandar.milosevic@example.com',
          phone: '+381 64 666 2222',
          rating: 4.5,
        },
        vehicleType: 'STANDARD',
        driverRating: 4.5,
        passengerRating: 4.7,
      },
      {
        id: '3',
        startTime: new Date(now.getTime() - 24 * 60 * 60 * 1000),
        endTime: new Date(now.getTime() - 23 * 60 * 60 * 1000),
        origin: 'Novi Sad Central Station',
        destination: 'Petrovaradin Fortress, Novi Sad',
        cost: 800,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: true,
        passengers: [
          {
            firstName: 'Milica',
            lastName: 'Stojanovic',
            email: 'milica.stojanovic@example.com',
            phone: '+381 64 777 8888',
          },
        ],
        driver: {
          firstName: 'Nikola',
          lastName: 'Ivanovic',
          email: 'nikola.ivanovic@example.com',
          phone: '+381 64 777 3333',
          rating: 3.9,
        },
        vehicleType: 'STANDARD',
        driverRating: 3.9,
        passengerRating: 4.1,
      },
      {
        id: '4',
        startTime: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000),
        endTime: null,
        origin: 'Belgrade Waterfront',
        destination: 'Zemun, Belgrade',
        cost: 0,
        isCancelled: true,
        cancelledBy: 'PASSENGER',
        cancellationReason: 'Passenger not found at pickup location',
        panicActivated: false,
        passengers: [
          {
            firstName: 'Jovan',
            lastName: 'Markovic',
            email: 'jovan.markovic@example.com',
            phone: '+381 64 999 0000',
          },
        ],
        driver: {
          firstName: 'Vladimir',
          lastName: 'Stevovic',
          email: 'vladimir.stevovic@example.com',
          phone: '+381 64 888 4444',
          rating: 4.3,
        },
        vehicleType: 'VAN',
      },
      {
        id: '5',
        startTime: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000),
        endTime: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000 + 45 * 60 * 1000),
        origin: 'Knez Mihailova Street, Belgrade',
        destination: 'Kalemegdan Fortress, Belgrade',
        cost: 950,
        isCancelled: false,
        cancelledBy: null,
        panicActivated: false,
        passengers: [
          {
            firstName: 'Sara',
            lastName: 'Popovic',
            email: 'sara.popovic@example.com',
            phone: '+381 64 111 3333',
          },
          {
            firstName: 'Luka',
            lastName: 'Djordjevic',
            email: 'luka.djordjevic@example.com',
            phone: '+381 64 222 4444',
          },
          {
            firstName: 'Jovana',
            lastName: 'Ilic',
            email: 'jovana.ilic@example.com',
            phone: '+381 64 333 5555',
          },
        ],
        driver: {
          firstName: 'Marija',
          lastName: 'Andric',
          email: 'marija.andric@example.com',
          phone: '+381 64 999 5555',
          rating: 4.6,
        },
        vehicleType: 'VAN',
        driverRating: 4.6,
        passengerRating: 4.8,
      },
    ];
  }
}
