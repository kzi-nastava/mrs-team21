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
  profilePicture?: string;
}

@Injectable({
  providedIn: 'root',
})
export class RegisterService {
  private apiUrl = `${environment.apiBaseUrl}/passengers`;

  constructor(private http: HttpClient) {}

  register(passenger: PassengerRegisterRequest): Observable<any> {
    return this.http.post(this.apiUrl, passenger);
  }
}
