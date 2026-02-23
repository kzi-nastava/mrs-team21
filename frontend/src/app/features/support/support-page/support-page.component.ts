import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { AuthService } from '../../../shared/services/auth.service';
import { SupportMessage } from '../models/support.model';
import { SupportApiService } from '../services/support-api.service';

@Component({
  selector: 'app-support-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support-page.component.html',
  styleUrl: './support-page.component.scss',
})
export class SupportPageComponent implements OnInit, OnDestroy {
  private readonly supportApi = inject(SupportApiService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private pollingTimer: ReturnType<typeof setInterval> | null = null;

  messages: SupportMessage[] = [];
  draftMessage = '';
  isLoading = true;
  isSending = false;
  errorMessage: string | null = null;

  private readonly pollingIntervalMs = 2500;
  private readonly requestTimeoutMs = 10000;
  readonly currentUserId = this.authService.getUserId();

  ngOnInit(): void {
    this.loadMessages(true);
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  sendMessage(): void {
    const content = this.draftMessage.trim();
    if (!content || this.isSending) {
      return;
    }

    const optimisticId = -Date.now();
    const optimistic: SupportMessage = {
      id: optimisticId,
      senderId: this.currentUserId ?? 0,
      senderName: '',
      senderSurname: '',
      receiverId: 0,
      receiverName: '',
      receiverSurname: '',
      content,
      createdAt: new Date().toISOString(),
    };
    this.messages = [...this.messages, optimistic];
    this.draftMessage = '';
    this.errorMessage = null;
    this.isSending = true;

    this.supportApi
      .sendMessage(content)
      .pipe(
        timeout(this.requestTimeoutMs),
        finalize(() => {
          this.isSending = false;
        }),
      )
      .subscribe({
        next: (createdMessage) => {
          this.messages = [
            ...this.messages.filter((m) => m.id !== optimisticId),
            createdMessage,
          ].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
          this.errorMessage = null;
        },
        error: (error) => {
          this.messages = this.messages.filter((m) => m.id !== optimisticId);
          this.errorMessage = error?.error?.message || 'Unable to send support message right now.';
        },
      });
  }

  isOwnMessage(message: SupportMessage): boolean {
    return this.currentUserId != null && message.senderId === this.currentUserId;
  }

  formatTime(isoTime: string): string {
    const date = new Date(isoTime);
    return new Intl.DateTimeFormat('en-GB', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
    }).format(date);
  }

  trackMessage(_: number, message: SupportMessage): number {
    return message.id;
  }

  private startPolling(): void {
    this.stopPolling();
    this.pollingTimer = setInterval(() => this.loadMessages(false), this.pollingIntervalMs);
  }

  private stopPolling(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }

  private loadMessages(showLoading: boolean): void {
    if (showLoading) {
      this.isLoading = true;
    }

    this.supportApi
      .getMyChat()
      .pipe(
        timeout(this.requestTimeoutMs),
        finalize(() => {
          this.isLoading = false;
        }),
      )
      .subscribe({
        next: (body) => {
          const list = Array.isArray(body) ? body : [];
          this.messages = list;
          this.isLoading = false;
          this.errorMessage = null;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error?.error?.message || 'Unable to load support messages.';
          this.cdr.markForCheck();
        },
      });
  }
}
