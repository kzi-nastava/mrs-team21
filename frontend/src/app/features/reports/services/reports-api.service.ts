import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ReportChartResponse } from '../models';

export interface DriverListItem {
  id: number;
  name: string;
  surname: string;
  email: string;
}

export interface UserSearchItem {
  id: number;
  email: string;
  name: string;
  surname: string;
}

export interface DriversPageResponse {
  content: DriverListItem[];
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class ReportsApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}`;

  getMyReport(from: string, to: string): Observable<ReportChartResponse> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<ReportChartResponse>(`${this.base}/reports/me`, { params });
  }

  getAdminReport(
    from: string,
    to: string,
    scope: 'all_drivers' | 'all_passengers' | 'user',
    userId?: number
  ): Observable<ReportChartResponse> {
    let params = new HttpParams().set('scope', scope);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    if (scope === 'user' && userId != null) params = params.set('userId', String(userId));
    return this.http.get<ReportChartResponse>(`${this.base}/admin/reports`, { params });
  }

  getDriversForPicker(page = 0, size = 500): Observable<DriversPageResponse> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', 'surname,asc');
    return this.http.get<DriversPageResponse>(`${this.base}/drivers`, { params });
  }

  /** Search users by email (admin only). Returns suggestions for report "one person" picker. */
  searchUsersByEmail(query: string, limit = 15): Observable<UserSearchItem[]> {
    if (!query?.trim()) return of([]);
    const params = new HttpParams().set('q', query.trim()).set('limit', String(limit));
    return this.http.get<UserSearchItem[]>(`${this.base}/admin/users/search`, { params });
  }
}
