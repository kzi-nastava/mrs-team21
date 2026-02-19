import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SupportConversationSummary, SupportMessage } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class SupportApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/support`;

  getMyChat(): Observable<SupportMessage[]> {
    return this.http.get<SupportMessage[]>(`${this.apiUrl}/my-chat`);
  }

  getConversations(): Observable<SupportConversationSummary[]> {
    return this.http.get<SupportConversationSummary[]>(`${this.apiUrl}/conversations`);
  }

  getConversation(userId: number): Observable<SupportMessage[]> {
    return this.http.get<SupportMessage[]>(`${this.apiUrl}/conversations/${userId}`);
  }

  sendMessage(content: string, receiverId?: number | null): Observable<SupportMessage> {
    return this.http.post<SupportMessage>(`${this.apiUrl}/messages`, {
      receiverId: receiverId ?? null,
      content,
    });
  }
}
