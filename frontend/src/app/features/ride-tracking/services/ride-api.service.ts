import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PageResponse, RideResponseDto } from '../../ride-history/models/ride-api.model';

@Injectable({ providedIn: 'root' })
export class RideApiService {
  private readonly http = inject(HttpClient);

  endRide(rideId: number): Observable<RideResponseDto> {
    return this.http.put<RideResponseDto>(`${environment.apiBaseUrl}/rides/${rideId}/end`, null);
  }

  getUpcomingDriverRides(driverId: number, page = 0, size = 5): Observable<RideResponseDto[]> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'scheduledFor,asc');

    return this.http
      .get<PageResponse<RideResponseDto>>(
        `${environment.apiBaseUrl}/drivers/${driverId}/rides/upcoming`,
        { params },
      )
      .pipe(map((response) => response.content));
  }
}
