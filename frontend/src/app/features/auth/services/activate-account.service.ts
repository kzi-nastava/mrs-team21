import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ActivateAccountService {
  constructor(private http: HttpClient) {}

  activate(token: string): Observable<void> {
    return this.http.get<void>(`${environment.apiBaseUrl}/passengers/activate/${token}`);
  }
}
