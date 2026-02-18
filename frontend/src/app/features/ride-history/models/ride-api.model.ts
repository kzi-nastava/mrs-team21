export interface RideWaypointInfoDto {
  locationId: number;
  address: string;
  lat: number;
  lng: number;
  order: number;
}

export interface RideResponseDto {
  id: number;
  status: string;
  requestedAt: string;
  scheduledFor?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  paidAt?: string | null;
  driverId?: number | null;
  driverName?: string | null;
  driverSurname?: string | null;
  vehicleId?: number | null;
  totalCost?: number | null;
  totalDistanceKm?: number | null;
  estimatedDurationSec?: number | null;
  estimatedArrivalAt?: string | null;
  babyTransport?: boolean | null;
  petTransport?: boolean | null;
  waypoints: RideWaypointInfoDto[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Location info for driver ride history response.
 */
export interface LocationInfoDto {
  address: string;
  lat: number;
  lng: number;
}

/**
 * Passenger info for driver ride history response.
 */
export interface PassengerInfoDto {
  id: number;
  name: string;
  surname: string;
}

/**
 * DTO for driver ride history items from backend.
 * Maps to DriverRideHistoryItemResponse.java
 */
export interface DriverRideHistoryItemDto {
  id: number;
  status: string;
  startTime: string | null;
  endTime: string | null;
  startLocation: LocationInfoDto | null;
  endLocation: LocationInfoDto | null;
  cancelled: boolean;
  canceledByUserId: number | null;
  canceledByName: string | null;
  canceledBySurname: string | null;
  totalCost: number | null;
  passengers: PassengerInfoDto[];
  panicOccurred: boolean;
}

/**
 * DTO for passenger ride history items from backend.
 * Maps to PassengerRideHistoryItemResponse.java
 */
export interface PassengerRideHistoryItemDto {
  id: number;
  status: string;
  requestedAt: string;
  scheduledFor: string | null;
  startTime: string | null;
  endTime: string | null;
  startAddress: string | null;
  destinationAddress: string | null;
  totalCost: number | null;
  canceled: boolean;
  canceledBy: string | null;
  hasPanic: boolean;
  isFavorite?: boolean;
  favoriteRouteId?: number | null;
}

export interface RideDetailsDriverDto {
  id: number;
  name: string;
  surname: string;
  email: string;
  phone: string;
  profilePictureUrl?: string | null;
}

export interface RideDetailsPassengerDto {
  id: number | null;
  name: string;
  surname: string;
  email: string;
}

export interface RideDetailsResponseDto {
  ride: RideResponseDto;
  driver: RideDetailsDriverDto | null;
  passengers: RideDetailsPassengerDto[];
  panicEvents: unknown[];
}
