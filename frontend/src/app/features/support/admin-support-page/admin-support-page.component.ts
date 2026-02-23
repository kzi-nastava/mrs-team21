import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { AuthService } from '../../../shared/services/auth.service';
import { SupportConversationSummary, SupportMessage } from '../models/support.model';
import { SupportApiService } from '../services/support-api.service';

@Component({
  selector: 'app-admin-support-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-support-page.component.html',
  styleUrl: './admin-support-page.component.scss',
})
export class AdminSupportPageComponent implements OnInit, OnDestroy {
  private readonly supportApi = inject(SupportApiService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private pollingTimer: ReturnType<typeof setInterval> | null = null;

  conversations: SupportConversationSummary[] = [];
  selectedUserId: number | null = null;
  messages: SupportMessage[] = [];
  draftMessage = '';

  isLoadingConversations = true;
  isLoadingMessages = false;
  isSending = false;
  errorMessage: string | null = null;

  private readonly pollingIntervalMs = 2500;
  private readonly requestTimeoutMs = 10000;
  readonly currentUserId = this.authService.getUserId();

  ngOnInit(): void {
    this.loadConversations(true);
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  get selectedConversation(): SupportConversationSummary | null {
    if (this.selectedUserId == null) {
      return null;
    }
    return this.conversations.find((item) => item.userId === this.selectedUserId) ?? null;
  }

  selectConversation(userId: number): void {
    if (this.selectedUserId === userId) {
      return;
    }
    this.selectedUserId = userId;
    this.loadMessages(true);
  }

  sendMessage(): void {
    const content = this.draftMessage.trim();
    if (!content || this.selectedUserId == null || this.isSending) {
      return;
    }

    const optimisticId = -Date.now();
    const selectedConv = this.selectedConversation;
    const optimistic: SupportMessage = {
      id: optimisticId,
      senderId: this.currentUserId ?? 0,
      senderName: '',
      senderSurname: '',
      receiverId: this.selectedUserId,
      receiverName: selectedConv?.userName ?? '',
      receiverSurname: selectedConv?.userSurname ?? '',
      content,
      createdAt: new Date().toISOString(),
    };
    this.messages = [...this.messages, optimistic];
    this.draftMessage = '';
    this.errorMessage = null;
    this.isSending = true;

    this.supportApi
      .sendMessage(content, this.selectedUserId)
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
          this.loadConversations(false);
        },
        error: (error) => {
          this.messages = this.messages.filter((m) => m.id !== optimisticId);
          this.errorMessage = error?.error?.message || 'Unable to send support reply right now.';
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

  trackConversation(_: number, conversation: SupportConversationSummary): number {
    return conversation.userId;
  }

  trackMessage(_: number, message: SupportMessage): number {
    return message.id;
  }

  private startPolling(): void {
    this.stopPolling();
    this.pollingTimer = setInterval(() => {
      this.loadConversations(false);
      this.loadMessages(false);
    }, this.pollingIntervalMs);
  }

  private stopPolling(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }

  private loadConversations(showLoading: boolean): void {
    if (showLoading) {
      this.isLoadingConversations = true;
    }

    this.supportApi
      .getConversations()
      .pipe(
        timeout(this.requestTimeoutMs),
        finalize(() => {
          this.isLoadingConversations = false;
        }),
      )
      .subscribe({
        next: (body) => {
          const list = Array.isArray(body) ? body : [];
          this.conversations = list;
          this.isLoadingConversations = false;
          this.errorMessage = null;

          if (list.length === 0) {
            this.selectedUserId = null;
            this.messages = [];
            this.cdr.markForCheck();
            return;
          }

          const selectedStillExists =
            this.selectedUserId != null &&
            list.some((conversation) => conversation.userId === this.selectedUserId);
          if (!selectedStillExists) {
            this.selectedUserId = list[0].userId;
            this.loadMessages(true);
          }
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isLoadingConversations = false;
          this.errorMessage = error?.error?.message || 'Unable to load support conversations.';
          this.cdr.markForCheck();
        },
      });
  }

  private loadMessages(showLoading: boolean): void {
    if (this.selectedUserId == null) {
      return;
    }
    if (showLoading) {
      this.isLoadingMessages = true;
    }

    this.supportApi
      .getConversation(this.selectedUserId)
      .pipe(
        timeout(this.requestTimeoutMs),
        finalize(() => {
          this.isLoadingMessages = false;
        }),
      )
      .subscribe({
        next: (body) => {
          const list = Array.isArray(body) ? body : [];
          this.messages = list;
          this.isLoadingMessages = false;
          this.errorMessage = null;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isLoadingMessages = false;
          this.errorMessage = error?.error?.message || 'Unable to load selected conversation.';
          this.cdr.markForCheck();
        },
      });
  }
}
