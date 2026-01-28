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
