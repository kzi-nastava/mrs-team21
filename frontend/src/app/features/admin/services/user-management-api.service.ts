import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../ride-history/models/ride-api.model';

export type UserRole = 'PASSENGER' | 'DRIVER' | 'ADMIN';

export interface UserResponse {
  id: number;
  name: string;
  surname: string;
  email: string;
  address: string | null;
  phone: string | null;
  profilePictureUrl: string | null;
  blocked: boolean;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class UserManagementApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  getUsers(
    page = 0,
    size = 10,
    sort = 'email,asc',
    role?: UserRole
  ): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', sort);
    if (role) {
      params = params.set('role', role);
    }
    return this.http.get<PageResponse<UserResponse>>(`${this.base}/admin/users`, {
      params,
    });
  }

  blockUser(id: number): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.base}/users/${id}/block`, {});
  }

  unblockUser(id: number): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.base}/users/${id}/unblock`, {});
  }
}
