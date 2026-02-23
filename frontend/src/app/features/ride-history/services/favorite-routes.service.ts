import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface FavoriteRouteWaypointDto {
  id: number;
  locationId: number;
  address: string;
  lat: number;
  lng: number;
  order: number;
}

export interface FavoriteRouteDto {
  id: number;
  passengerId: number;
  vehicleTypeId: number;
  vehicleTypeName: string;
  babyTransport: boolean;
  petTransport: boolean;
  waypoints: FavoriteRouteWaypointDto[];
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class FavoriteRoutesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

  getByPassenger(passengerId: number): Observable<FavoriteRouteDto[]> {
    return this.http.get<FavoriteRouteDto[]>(
      `${this.apiUrl}/passengers/${passengerId}/favorite-routes`
    );
  }

  createFromRide(passengerId: number, rideId: number): Observable<FavoriteRouteDto> {
    return this.http.post<FavoriteRouteDto>(
      `${this.apiUrl}/passengers/${passengerId}/favorite-routes/from-ride/${rideId}`,
      {}
    );
  }

  deleteById(favoriteRouteId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/favorite-routes/${favoriteRouteId}`
    );
  }

  deleteByRide(passengerId: number, rideId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/passengers/${passengerId}/favorite-routes/by-ride/${rideId}`
    );
  }
}
