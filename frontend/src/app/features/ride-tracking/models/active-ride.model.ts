/**
 * Models for active ride tracking
 */

export interface ActiveRide {
  id: string;
  status: string;
  startAddress: string;
  destinationAddress: string;
  startLocation: {
    lat: number;
    lng: number;
  };
  destinationLocation: {
    lat: number;
    lng: number;
  };
  currentLocation: {
    lat: number;
    lng: number;
  };
  driver: {
    id: number;
    firstName: string;
    lastName: string;
    phone: string;
    profilePictureUrl?: string;
  };
  vehicle: {
    id: number;
    model: string;
    licensePlate: string;
    vehicleType: 'STANDARD' | 'LUXURY' | 'VAN';
  };
  estimatedArrivalTime: number; // in seconds
  startTime: Date;
  route?: RouteWaypoint[];
}

export interface RouteWaypoint {
  lat: number;
  lng: number;
  order: number;
}

export interface RideInconsistencyReport {
  id: string;
  rideId: string;
  note: string;
  reportedAt: Date;
  reportedBy: {
    firstName: string;
    lastName: string;
    email: string;
  };
}

export interface LocationUpdate {
  lat: number;
  lng: number;
  timestamp: Date;
  estimatedArrivalTime: number; // in seconds
  bearing?: number; // Bearing in degrees (0-360)
}
