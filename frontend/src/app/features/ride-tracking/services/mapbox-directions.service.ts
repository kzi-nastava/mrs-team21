import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';

type LngLat = { lat: number; lng: number };

interface DirectionsResponse {
  code: string;
  routes: Array<{
    geometry: {
      type: 'LineString';
      coordinates: [number, number][];
    };
  }>;
}

@Injectable({ providedIn: 'root' })
export class MapboxDirectionsService {
  private readonly baseUrl = 'https://api.mapbox.com/directions/v5/mapbox';

  constructor(private http: HttpClient) {}

  getRoute(from: LngLat, to: LngLat): Observable<[number, number][]> {
    return this.requestRoute(from, to, 'driving-traffic').pipe(
      catchError(() => this.requestRoute(from, to, 'driving'))
    );
  }

  private requestRoute(
    from: LngLat,
    to: LngLat,
    profile: 'driving-traffic' | 'driving'
  ): Observable<[number, number][]> {
    const token = environment.mapboxToken;
    if (!token || token === 'MAPBOX_API_KEY' || token.trim() === '') {
      return throwError(() => new Error('Mapbox API key is missing or invalid.'));
    }

    const coordinates = `${from.lng},${from.lat};${to.lng},${to.lat}`;
    const params = new HttpParams()
      .set('geometries', 'geojson')
      .set('overview', 'full')
      .set('access_token', token);

    return this.http
      .get<DirectionsResponse>(`${this.baseUrl}/${profile}/${coordinates}`, {
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
        })
      );
  }
}
