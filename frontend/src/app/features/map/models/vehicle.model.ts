/**
 * Vehicle model interfaces matching backend DTO structure
 */

export interface VehicleResponse {
  id: number;
  driverId: number;
  driverName: string;
  driverSurname: string;
  vehicleTypeId: number;
  vehicleTypeName: string;
  model: string;
  licensePlate: string;
  numSeats: number;
  babyFriendly: boolean;
  petFriendly: boolean;
  currentLat: number;
  currentLng: number;
  available: boolean;
}

export interface MapMarker {
  lat: number;
  lng: number;
  status: 'available' | 'busy';
  driverName?: string;
  vehicleType?: 'car' | 'van' | 'motorcycle';
}

/**
 * Vehicle type mapping from backend enum to MapMarker vehicleType
 */
const VEHICLE_TYPE_MAP: Record<string, MapMarker['vehicleType']> = {
  STANDARD: 'car',
  LUXURY: 'car',
  VAN: 'van',
  MOTORCYCLE: 'motorcycle',
};

/**
 * Transforms a VehicleResponse to a MapMarker
 */
export function vehicleToMapMarker(vehicle: VehicleResponse): MapMarker {
  const typeKey = vehicle.vehicleTypeName
    ? vehicle.vehicleTypeName.toUpperCase()
    : '';
  return {
    lat: vehicle.currentLat,
    lng: vehicle.currentLng,
    status: vehicle.available ? 'available' : 'busy',
    driverName: `${vehicle.driverName} ${vehicle.driverSurname}`,
    vehicleType: VEHICLE_TYPE_MAP[typeKey] ?? 'car',
  };
}
