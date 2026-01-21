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
 * Transforms a VehicleResponse to a MapMarker
 */
export function vehicleToMapMarker(vehicle: VehicleResponse): MapMarker {
  return {
    lat: vehicle.currentLat,
    lng: vehicle.currentLng,
    status: vehicle.available ? 'available' : 'busy',
    driverName: `${vehicle.driverName} ${vehicle.driverSurname}`,
    vehicleType: vehicle.vehicleTypeName?.toLowerCase().includes('van')
      ? 'van'
      : vehicle.vehicleTypeName?.toLowerCase().includes('motorcycle')
      ? 'motorcycle'
      : 'car',
  };
}
