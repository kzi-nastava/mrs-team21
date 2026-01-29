import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GeocodingCore } from '@mapbox/search-js-core';
import { Observable, forkJoin, from, map, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  EstimateRequest,
  EstimateResponse,
  LocationDTO,
  VehicleTypeName,
} from '../models/estimate.model';

@Injectable({
  providedIn: 'root',
})
export class EstimateService {
  private apiUrl = `${environment.apiBaseUrl}/rides/estimate`;
  private geocodingClient: GeocodingCore;

  constructor(private http: HttpClient) {
    this.geocodingClient = new GeocodingCore({ accessToken: environment.mapboxToken });
  }

  getEstimate(
    startAddress: string,
    destinationAddress: string,
    vehicleType: VehicleTypeName = VehicleTypeName.STANDARD,
  ): Observable<EstimateResponse> {
    // Geocode both addresses
    const startGeocode$ = this.geocodeAddress(startAddress);
    const destGeocode$ = this.geocodeAddress(destinationAddress);

    return forkJoin([startGeocode$, destGeocode$]).pipe(
      switchMap(([startLocation, destinationLocation]) => {
        const request: EstimateRequest = {
          startLocation,
          destinationLocation,
          waypoints: [],
          vehicleTypeName: vehicleType,
        };
        return this.http.post<EstimateResponse>(this.apiUrl, request).pipe(
          map((response: EstimateResponse) => {
            const priceNum = Number(response.estimatedPrice);
            const estimatedPrice = Number.isFinite(priceNum) ? Math.round(priceNum) : 0;
            return { ...response, estimatedPrice } as EstimateResponse;
          }),
        );
      }),
    );
  }

  private geocodeAddress(address: string): Observable<LocationDTO> {
    return from(this.geocodingClient.forward(address, { limit: 1 })).pipe(
      map((response: any) => {
        const feature = response?.features?.[0];
        if (!feature) throw new Error('Geocoding failed: No results');
        const coords = feature?.properties?.coordinates;
        return {
          latitude: coords?.latitude ?? feature.geometry.coordinates[1],
          longitude: coords?.longitude ?? feature.geometry.coordinates[0],
          address: feature?.properties?.full_address ?? feature?.properties?.name ?? address,
        } as LocationDTO;
      }),
    );
  }
}
