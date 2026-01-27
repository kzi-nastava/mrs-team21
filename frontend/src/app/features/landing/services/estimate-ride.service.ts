import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  private geocodingClient: any;
  private geocodingInitPromise: Promise<void>;

  constructor(private http: HttpClient) {
    this.geocodingInitPromise = this.initGeocoding();
  }

  private async initGeocoding(): Promise<void> {
    const mbxGeocoding = (await import('@mapbox/mapbox-sdk/services/geocoding')).default;
    this.geocodingClient = mbxGeocoding({ accessToken: environment.mapboxToken });
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
    const promise = this.geocodingInitPromise.then(() =>
      this.geocodingClient.forwardGeocode({ query: address, limit: 1 }).send()
    );
    return from(promise).pipe(
      map((response: any) => {
        const feature = response?.body?.features?.[0];
        if (!feature) throw new Error('Geocoding failed: No results');
        return {
          latitude: feature.center[1],
          longitude: feature.center[0],
          address: feature.place_name,
        } as LocationDTO;
      }),
    );
  }
}
