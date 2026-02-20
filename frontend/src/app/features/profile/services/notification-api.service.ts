import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UserNotification } from '../models/notification.model';

export interface NotificationPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly http = inject(HttpClient);

  getUserNotifications(userId: number, page = 0, size = 10): Observable<UserNotification[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<NotificationPageResponse<UserNotification>>(
        `${environment.apiBaseUrl}/users/${userId}/notifications`,
        { params },
      )
      .pipe(map((response) => response.content));
  }

  /** Returns full page (for admin or when pagination UI is needed). */
  getUserNotificationsPage(
    userId: number,
    page = 0,
    size = 10
  ): Observable<NotificationPageResponse<UserNotification>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<NotificationPageResponse<UserNotification>>(
      `${environment.apiBaseUrl}/users/${userId}/notifications`,
      { params }
    );
  }
}
