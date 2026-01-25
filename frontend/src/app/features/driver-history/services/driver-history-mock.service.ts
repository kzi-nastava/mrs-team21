import { Injectable } from '@angular/core';
import { RideHistory } from '../models/ride-history.model';

@Injectable({ providedIn: 'root' })
export class DriverHistoryMockService {
  getRideHistory(): RideHistory[] {
    const now = new Date();
    
    return [
      {
        id: '1',
        startTime: new Date(now.getTime() - 2 * 60 * 60 * 1000), // 2 hours ago
        endTime: new Date(now.getTime() - 1 * 60 * 60 * 1000), // 1 hour ago
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
        vehicleType: 'LUXURY',
      },
      {
        id: '2',
        startTime: new Date(now.getTime() - 5 * 60 * 60 * 1000), // 5 hours ago
        endTime: new Date(now.getTime() - 4 * 60 * 60 * 1000), // 4 hours ago
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
        vehicleType: 'STANDARD',
      },
      {
        id: '3',
        startTime: new Date(now.getTime() - 24 * 60 * 60 * 1000), // 1 day ago
        endTime: new Date(now.getTime() - 23 * 60 * 60 * 1000), // 23 hours ago
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
        vehicleType: 'STANDARD',
      },
      {
        id: '4',
        startTime: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
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
        vehicleType: 'VAN',
      },
      {
        id: '5',
        startTime: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000), // 3 days ago
        endTime: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000 + 45 * 60 * 1000), // 45 min later
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
        vehicleType: 'VAN',
      },
    ];
  }
}

