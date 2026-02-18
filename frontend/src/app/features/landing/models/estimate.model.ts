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
  /** Route line as [lng, lat] pairs for map drawing. Mapbox order. */
  routeCoordinates?: [number, number][];
  distanceInKm: number;
  durationInMinutes: number;
  estimatedPrice: number;
}

/** Result of getEstimate including geocoded waypoints in order [start, ...stops, destination] for ride create. */
export interface EstimateResultWithWaypoints extends EstimateResponse {
  geocodedWaypoints: LocationDTO[];
}
