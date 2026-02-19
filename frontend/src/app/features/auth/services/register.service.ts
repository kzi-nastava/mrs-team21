import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface PassengerRegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  address: string;
  phoneNumber: string;
  profilePictureUrl?: string;
}

@Injectable({
  providedIn: 'root',
})
export class RegisterService {
  private apiUrl = `${environment.apiBaseUrl}/passengers`;
  private authUrl = `${environment.apiBaseUrl}/auth`;

  constructor(private http: HttpClient) {}

  register(passenger: PassengerRegisterRequest): Observable<unknown> {
    return this.http.post(this.apiUrl, passenger);
  }

  uploadProfilePicture(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.set('file', file);
    return this.http.post<{ url: string }>(`${this.authUrl}/profile-picture`, formData);
  }
}
