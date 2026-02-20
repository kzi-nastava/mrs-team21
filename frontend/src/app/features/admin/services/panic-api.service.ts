import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface PanicEventDto {
  id: number;
  rideId: number;
  userId: number;
  userEmail: string;
  reason: string | null;
  createdAt: string;
}

export interface PanicEventsPageResponse {
  content: PanicEventDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class PanicApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/panic-events`;

  getPanicEventsPage(
    page = 0,
    size = 10,
    sort = 'createdAt,desc'
  ): Observable<PanicEventsPageResponse> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', sort);
    return this.http.get<PanicEventsPageResponse>(this.baseUrl, { params });
  }
}
