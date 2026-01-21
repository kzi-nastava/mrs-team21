import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { VehicleResponse } from '../models/vehicle.model';

/**
 * Mock service for vehicle data
 * Provides realistic test data for active vehicles around Novi Sad
 * Pattern follows driver-history-mock.service.ts
 */
@Injectable({ providedIn: 'root' })
export class VehicleMockService {
  private mockVehicles: VehicleResponse[] = [
    {
      id: 1,
      driverId: 101,
      driverName: 'Marko',
      driverSurname: 'Petrović',
      vehicleTypeId: 1,
      vehicleTypeName: 'STANDARD',
      model: 'Toyota Corolla',
      licensePlate: 'NS-123-AB',
      numSeats: 4,
      babyFriendly: false,
      petFriendly: true,
      currentLat: 45.2671,
      currentLng: 19.8335, // City center
      available: true,
    },
    {
      id: 2,
      driverId: 102,
      driverName: 'Ana',
      driverSurname: 'Jovanović',
      vehicleTypeId: 2,
      vehicleTypeName: 'LUXURY',
      model: 'Mercedes-Benz E-Class',
      licensePlate: 'NS-456-CD',
      numSeats: 4,
      babyFriendly: true,
      petFriendly: true,
      currentLat: 45.2556,
      currentLng: 19.8447, // Liman area
      available: true,
    },
    {
      id: 3,
      driverId: 103,
      driverName: 'Stefan',
      driverSurname: 'Nikolić',
      vehicleTypeId: 1,
      vehicleTypeName: 'STANDARD',
      model: 'Volkswagen Golf',
      licensePlate: 'NS-789-EF',
      numSeats: 4,
      babyFriendly: false,
      petFriendly: false,
      currentLat: 45.2517,
      currentLng: 19.8369, // Petrovaradin
      available: false, // Currently on a ride
    },
    {
      id: 4,
      driverId: 104,
      driverName: 'Milica',
      driverSurname: 'Stojanović',
      vehicleTypeId: 3,
      vehicleTypeName: 'VAN',
      model: 'Ford Transit',
      licensePlate: 'NS-321-GH',
      numSeats: 8,
      babyFriendly: true,
      petFriendly: true,
      currentLat: 45.2805,
      currentLng: 19.8203, // Detelinara area
      available: true,
    },
    {
      id: 5,
      driverId: 105,
      driverName: 'Jovan',
      driverSurname: 'Marković',
      vehicleTypeId: 1,
      vehicleTypeName: 'STANDARD',
      model: 'Peugeot 308',
      licensePlate: 'NS-654-IJ',
      numSeats: 4,
      babyFriendly: false,
      petFriendly: true,
      currentLat: 45.2432,
      currentLng: 19.8015, // Sremska Kamenica
      available: true,
    },
    {
      id: 6,
      driverId: 106,
      driverName: 'Sara',
      driverSurname: 'Popović',
      vehicleTypeId: 2,
      vehicleTypeName: 'LUXURY',
      model: 'BMW 5 Series',
      licensePlate: 'NS-987-KL',
      numSeats: 4,
      babyFriendly: true,
      petFriendly: false,
      currentLat: 45.2734,
      currentLng: 19.8578, // Grbavica area
      available: false, // Currently on a ride
    },
    {
      id: 7,
      driverId: 107,
      driverName: 'Luka',
      driverSurname: 'Đorđević',
      vehicleTypeId: 1,
      vehicleTypeName: 'STANDARD',
      model: 'Renault Clio',
      licensePlate: 'NS-147-MN',
      numSeats: 4,
      babyFriendly: false,
      petFriendly: false,
      currentLat: 45.2598,
      currentLng: 19.8124, // Telep area
      available: true,
    },
    {
      id: 8,
      driverId: 108,
      driverName: 'Jovana',
      driverSurname: 'Ilić',
      vehicleTypeId: 3,
      vehicleTypeName: 'VAN',
      model: 'Mercedes Sprinter',
      licensePlate: 'NS-258-OP',
      numSeats: 8,
      babyFriendly: true,
      petFriendly: true,
      currentLat: 45.2645,
      currentLng: 19.8489, // Rotkvarija area
      available: true,
    },
    {
      id: 9,
      driverId: 109,
      driverName: 'Nikola',
      driverSurname: 'Radović',
      vehicleTypeId: 1,
      vehicleTypeName: 'STANDARD',
      model: 'Opel Astra',
      licensePlate: 'NS-369-QR',
      numSeats: 4,
      babyFriendly: false,
      petFriendly: true,
      currentLat: 45.2487,
      currentLng: 19.8291, // Near Danube
      available: false, // Currently on a ride
    },
    {
      id: 10,
      driverId: 110,
      driverName: 'Marija',
      driverSurname: 'Tomić',
      vehicleTypeId: 2,
      vehicleTypeName: 'LUXURY',
      model: 'Audi A6',
      licensePlate: 'NS-741-ST',
      numSeats: 4,
      babyFriendly: true,
      petFriendly: true,
      currentLat: 45.2712,
      currentLng: 19.8415, // City center area
      available: true,
    },
  ];

  /**
   * Returns mock active vehicles as Observable
   */
  getActiveVehicles(): Observable<VehicleResponse[]> {
    return of(this.mockVehicles);
  }

  /**
   * Returns mock active vehicles as array (synchronous)
   */
  getActiveVehiclesSync(): VehicleResponse[] {
    return this.mockVehicles;
  }
}
