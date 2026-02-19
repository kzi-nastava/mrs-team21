import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface VehicleTypeResponse {
  id: number;
  name: string;
  startPrice: number;
  pricePerKm: number;
}

export interface VehicleTypeUpdateRequest {
  startPrice: number;
  pricePerKm: number;
}

@Injectable({
  providedIn: 'root',
})
export class VehicleTypePricingService {
  private readonly apiUrl = `${environment.apiBaseUrl}/vehicle-types`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<VehicleTypeResponse[]> {
    return this.http.get<VehicleTypeResponse[]>(this.apiUrl).pipe(timeout(15000));
  }

  update(id: number, request: VehicleTypeUpdateRequest): Observable<VehicleTypeResponse> {
    return this.http
      .put<VehicleTypeResponse>(`${this.apiUrl}/${id}`, request)
      .pipe(timeout(15000));
  }
}
