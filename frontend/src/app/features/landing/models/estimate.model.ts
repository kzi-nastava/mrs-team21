export interface LocationDTO {
  latitude: number;
  longitude: number;
  address?: string;
}

export interface EstimateRequest {
  startLocation: LocationDTO;
  destinationLocation: LocationDTO;
  waypoints?: LocationDTO[];
  vehicleTypeName?: VehicleTypeName;
}

export enum VehicleTypeName {
  STANDARD = 'STANDARD',
  LUXURY = 'LUXURY',
  VAN = 'VAN',
}

export interface EstimateResponse {
  routePolyline: string;
  distanceInKm: number;
  durationInMinutes: number;
  estimatedPrice: number;
}
