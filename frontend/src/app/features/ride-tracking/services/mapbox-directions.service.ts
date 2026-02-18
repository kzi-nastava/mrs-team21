import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';

interface LngLat {
  lat: number;
  lng: number;
}

interface DirectionsResponse {
  code: string;
  routes: {
    geometry: {
      type: 'LineString';
      coordinates: [number, number][];
    };
  }[];
}

@Injectable({ providedIn: 'root' })
export class MapboxDirectionsService {
  private readonly baseUrl = 'https://api.mapbox.com/directions/v5/mapbox';

  constructor(private http: HttpClient) {}

  /** Route between two points. */
  getRoute(from: LngLat, to: LngLat): Observable<[number, number][]> {
    return this.getRouteWithWaypoints([from, to]);
  }

  /**
   * Route through multiple waypoints (2–25 points). Returns full road-aligned geometry.
   */
  getRouteWithWaypoints(
    coordinates: Array<{ lat: number; lng: number }>,
  ): Observable<[number, number][]> {
    if (coordinates.length < 2) {
      return throwError(() => new Error('At least 2 waypoints required.'));
    }
    if (coordinates.length > 25) {
      return throwError(() => new Error('At most 25 waypoints allowed.'));
    }
    return this.requestRouteWithCoordinates(coordinates, 'driving-traffic').pipe(
      catchError(() => this.requestRouteWithCoordinates(coordinates, 'driving')),
    );
  }

  private requestRouteWithCoordinates(
    coordinates: Array<{ lat: number; lng: number }>,
    profile: 'driving-traffic' | 'driving',
  ): Observable<[number, number][]> {
    const token = environment.mapboxToken;
    if (!token || token === 'MAPBOX_API_KEY' || token.trim() === '') {
      return throwError(() => new Error('Mapbox API key is missing or invalid.'));
    }

    const coordinatesStr = coordinates
      .map((c) => `${c.lng},${c.lat}`)
      .join(';');
    const params = new HttpParams()
      .set('geometries', 'geojson')
      .set('overview', 'full')
      .set('access_token', token);

    return this.http
      .get<DirectionsResponse>(`${this.baseUrl}/${profile}/${coordinatesStr}`, {
        params,
      })
      .pipe(
        map((response) => {
          if (!response || response.code !== 'Ok' || !response.routes?.length) {
            throw new Error('Mapbox directions response is invalid.');
          }

          const geometry = response.routes[0].geometry;
          if (
            !geometry ||
            geometry.type !== 'LineString' ||
            geometry.coordinates.length < 2
          ) {
            throw new Error('Mapbox route geometry is missing.');
          }

          return geometry.coordinates;
        }),
      );
  }
}
