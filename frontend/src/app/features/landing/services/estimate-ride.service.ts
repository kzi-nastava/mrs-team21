import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GeocodingCore, SearchBoxCore, SessionToken } from '@mapbox/search-js-core';
import { Observable, catchError, forkJoin, from, map, of, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  EstimateRequest,
  EstimateResponse,
  EstimateResultWithWaypoints,
  LocationDTO,
  VehicleTypeName,
} from '../models/estimate.model';

/** One suggestion item for address autocomplete. */
export interface AddressSuggestion {
  address: string;
}

@Injectable({
  providedIn: 'root',
})
export class EstimateService {
  private apiUrl = `${environment.apiBaseUrl}/rides/estimate`;
  private geocodingClient: GeocodingCore;
  private searchBox: SearchBoxCore;

  constructor(private http: HttpClient) {
    this.geocodingClient = new GeocodingCore({ accessToken: environment.mapboxToken });
    this.searchBox = new SearchBoxCore({
      accessToken: environment.mapboxToken,
      proximity: { longitude: 19.82, latitude: 45.25 }, // Novi Sad
      limit: 5,
    });
  }

  /**
   * Get address suggestions while the user types. Tries Mapbox Search Box API first;
   * if that fails or returns empty (e.g. token has no Search Box scope), falls back
   * to the Geocoding API, which works with the standard "geocode locations" token.
   */
  getAddressSuggestions(query: string): Observable<AddressSuggestion[]> {
    const q = (query || '').trim();
    if (q.length < 2) return of([]);
    const token = environment.mapboxToken;
    if (!token || token === 'MAPBOX_API_KEY' || token.trim() === '') {
      console.warn('Address suggestions: Mapbox token not set. Add your token in environment.dev.ts (mapboxToken).');
      return of([]);
    }
    const sessionToken = new SessionToken();
    return from(this.searchBox.suggest(q, { sessionToken })).pipe(
      map((res) => {
        const list = res?.suggestions ?? [];
        return list
          .map((s: { full_address?: string; name?: string; address?: string; place_formatted?: string }) => {
            const address =
              s.full_address ||
              s.name ||
              (s.address && s.place_formatted ? `${s.address}, ${s.place_formatted}` : s.address) ||
              '';
            return { address };
          })
          .filter((s) => s.address.length > 0);
      }),
      switchMap((list) => (list.length > 0 ? of(list) : this.suggestViaGeocoding(q))),
      catchError(() => this.suggestViaGeocoding(q)),
    );
  }

  /** Fallback: use Geocoding API (forward) so suggestions work with tokens that only have "geocode locations". */
  private suggestViaGeocoding(query: string): Observable<AddressSuggestion[]> {
    return from(this.geocodingClient.forward(query, { limit: 5 })).pipe(
      map((res: { features?: Array<{ properties?: { place_name?: string; name?: string } }> }) => {
        const features = res?.features ?? [];
        return features
          .map((f) => {
            const name = f?.properties?.place_name ?? f?.properties?.name ?? '';
            return { address: name };
          })
          .filter((s) => s.address.length > 0);
      }),
      catchError(() => of([])),
    );
  }

  getEstimate(
    startAddress: string,
    destinationAddress: string,
    vehicleType: VehicleTypeName = VehicleTypeName.STANDARD,
    waypointAddresses: string[] = [],
  ): Observable<EstimateResponse> {
    const waypointGeocodes$ =
      waypointAddresses.length > 0
        ? forkJoin(waypointAddresses.map((addr) => this.geocodeAddress(addr)))
        : of([]);
    const startGeocode$ = this.geocodeAddress(startAddress);
    const destGeocode$ = this.geocodeAddress(destinationAddress);

    return forkJoin({
      start: startGeocode$,
      destination: destGeocode$,
      waypoints: waypointGeocodes$,
    }).pipe(
      switchMap(({ start: startLocation, destination: destinationLocation, waypoints: waypointLocations }) => {
        const request: EstimateRequest = {
          startLocation,
          destinationLocation,
          waypoints: (waypointLocations as LocationDTO[]) ?? [],
          vehicleTypeName: vehicleType,
        };
        return this.http.post<EstimateResponse>(this.apiUrl, request).pipe(
          map((response: EstimateResponse) => {
            const priceNum = Number(response.estimatedPrice);
            const estimatedPrice = Number.isFinite(priceNum) ? Math.round(priceNum) : 0;
            const routeCoordinates = this.normalizeRouteCoordinates(response.routeCoordinates);
            return { ...response, estimatedPrice, routeCoordinates } as EstimateResponse;
          }),
        );
      }),
    );
  }

  /**
   * Same as getEstimate but returns geocoded waypoints in order [start, ...stops, destination]
   * for building RideCreateRequest (e.g. in order-ride confirm).
   */
  getEstimateWithWaypoints(
    startAddress: string,
    destinationAddress: string,
    vehicleType: VehicleTypeName = VehicleTypeName.STANDARD,
    waypointAddresses: string[] = [],
  ): Observable<EstimateResultWithWaypoints> {
    const waypointGeocodes$ =
      waypointAddresses.length > 0
        ? forkJoin(waypointAddresses.map((addr) => this.geocodeAddress(addr)))
        : of([]);
    const startGeocode$ = this.geocodeAddress(startAddress);
    const destGeocode$ = this.geocodeAddress(destinationAddress);

    return forkJoin({
      start: startGeocode$,
      destination: destGeocode$,
      waypoints: waypointGeocodes$,
    }).pipe(
      switchMap(({ start: startLocation, destination: destinationLocation, waypoints: waypointLocations }) => {
        const waypoints = (waypointLocations as LocationDTO[]) ?? [];
        const request: EstimateRequest = {
          startLocation,
          destinationLocation,
          waypoints,
          vehicleTypeName: vehicleType,
        };
        return this.http.post<EstimateResponse>(this.apiUrl, request).pipe(
          map((response: EstimateResponse) => {
            const priceNum = Number(response.estimatedPrice);
            const estimatedPrice = Number.isFinite(priceNum) ? Math.round(priceNum) : 0;
            const routeCoordinates = this.normalizeRouteCoordinates(response.routeCoordinates);
            const geocodedWaypoints: LocationDTO[] = [
              startLocation,
              ...waypoints,
              destinationLocation,
            ];
            return {
              ...response,
              estimatedPrice,
              routeCoordinates,
              geocodedWaypoints,
            } as EstimateResultWithWaypoints;
          }),
        );
      }),
    );
  }

  /** Ensure route coordinates are [lng, lat][] for map component. */
  private normalizeRouteCoordinates(coords: number[][] | undefined): [number, number][] | undefined {
    if (!coords || !Array.isArray(coords) || coords.length === 0) return undefined;
    return coords
      .filter((p): p is number[] => Array.isArray(p) && p.length >= 2)
      .map((p) => [Number(p[0]), Number(p[1])] as [number, number]);
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
