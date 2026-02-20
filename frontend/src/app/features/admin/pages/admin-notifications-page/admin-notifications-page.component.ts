import {
  Component,
  inject,
  signal,
  computed,
  DestroyRef,
  OnInit,
  effect,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReportsApiService,
  UserSearchItem,
} from '../../../reports/services/reports-api.service';
import {
  NotificationApiService,
  NotificationPageResponse,
} from '../../../profile/services/notification-api.service';
import { UserNotification } from '../../../profile/models/notification.model';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

const PAGE_SIZE_OPTIONS = [10, 25, 50];

const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  RIDE_ACCEPTED: 'Ride accepted',
  RIDE_REJECTED: 'Ride rejected',
  RIDE_STARTED: 'Ride started',
  RIDE_FINISHED: 'Ride finished',
  RIDE_CANCELLED: 'Ride cancelled',
  LINKED_TO_RIDE: 'Linked to ride',
  PANIC_ALERT: 'Panic alert',
  SUPPORT_MESSAGE: 'Support message',
  SCHEDULED_RIDE_REMINDER: 'Scheduled ride reminder',
};

function formatNotificationDate(iso: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    dateStyle: 'short',
    timeStyle: 'short',
  });
}

@Component({
  selector: 'app-admin-notifications-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-notifications-page.component.html',
  styleUrl: './admin-notifications-page.component.scss',
})
export class AdminNotificationsPageComponent implements OnInit {
  private readonly reportsApi = inject(ReportsApiService);
  private readonly notificationApi = inject(NotificationApiService);
  private readonly destroyRef = inject(DestroyRef);

  emailSearchQuery = signal('');
  userSuggestions = signal<UserSearchItem[]>([]);
  selectedUser = signal<UserSearchItem | null>(null);
  loadingSuggestions = signal(false);
  showSuggestions = signal(false);
  private searchSubject = new Subject<string>();
  private suggestionBlurTimeout: ReturnType<typeof setTimeout> | null = null;

  notificationsPage = signal<NotificationPageResponse<UserNotification> | null>(
    null
  );
  loadingNotifications = signal(false);
  notificationsError = signal<string | null>(null);
  currentPage = signal(0);
  pageSize = signal(10);

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  private loadNotificationsEffect = effect(() => {
    const user = this.selectedUser();
    const page = this.currentPage();
    const size = this.pageSize();
    if (!user) {
      this.notificationsPage.set(null);
      this.notificationsError.set(null);
      this.loadingNotifications.set(false);
      return;
    }
    const requestKey = `${user.id}-${page}-${size}`;
    this.loadingNotifications.set(true);
    this.notificationsError.set(null);
    this.notificationApi
      .getUserNotificationsPage(user.id, page, size)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (requestKey === `${this.selectedUser()?.id}-${this.currentPage()}-${this.pageSize()}`) {
            this.notificationsPage.set(res);
          }
          this.loadingNotifications.set(false);
        },
        error: (err) => {
          if (requestKey === `${this.selectedUser()?.id}-${this.currentPage()}-${this.pageSize()}`) {
            this.notificationsError.set(
              err.error?.message || 'Failed to load notifications.'
            );
            this.notificationsPage.set(null);
          }
          this.loadingNotifications.set(false);
        },
      });
  });

  notifications = computed(() => this.notificationsPage()?.content ?? []);
  totalElements = computed(() => this.notificationsPage()?.totalElements ?? 0);
  totalPages = computed(() => this.notificationsPage()?.totalPages ?? 0);
  hasNext = computed(
    () => this.currentPage() < this.totalPages() - 1 && this.totalPages() > 0
  );
  hasPrev = computed(() => this.currentPage() > 0);

  ngOnInit(): void {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          this.loadingSuggestions.set(true);
          return this.reportsApi.searchUsersByEmail(q, 15);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (list) => {
          this.userSuggestions.set(list);
          this.loadingSuggestions.set(false);
          this.showSuggestions.set(list.length > 0);
        },
        error: () => {
          this.userSuggestions.set([]);
          this.loadingSuggestions.set(false);
        },
      });
  }

  onEmailSearchInput(value: string): void {
    this.emailSearchQuery.set(value);
    this.selectedUser.set(null);
    const trimmed = value?.trim() ?? '';
    if (trimmed.length < 2) {
      this.userSuggestions.set([]);
      this.showSuggestions.set(false);
      return;
    }
    this.searchSubject.next(trimmed);
  }

  selectUser(user: UserSearchItem): void {
    this.selectedUser.set(user);
    this.emailSearchQuery.set(user.email);
    this.userSuggestions.set([]);
    this.showSuggestions.set(false);
    this.currentPage.set(0);
  }

  clearSelectedUser(): void {
    this.selectedUser.set(null);
    this.emailSearchQuery.set('');
    this.userSuggestions.set([]);
    this.showSuggestions.set(false);
    this.notificationsPage.set(null);
  }

  onEmailSearchBlur(): void {
    this.suggestionBlurTimeout = setTimeout(
      () => this.showSuggestions.set(false),
      150
    );
  }

  onEmailSearchFocus(): void {
    if (this.suggestionBlurTimeout) {
      clearTimeout(this.suggestionBlurTimeout);
      this.suggestionBlurTimeout = null;
    }
    if (this.emailSearchQuery() && this.userSuggestions().length)
      this.showSuggestions.set(true);
  }

  userLabel(u: UserSearchItem): string {
    return `${u.name} ${u.surname} (${u.email})`;
  }

  typeLabel(type: string): string {
    return NOTIFICATION_TYPE_LABELS[type] ?? type;
  }

  formatDate(iso: string): string {
    return formatNotificationDate(iso);
  }

  setPageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  nextPage(): void {
    if (this.hasNext()) this.currentPage.update((p) => p + 1);
  }

  prevPage(): void {
    if (this.hasPrev()) this.currentPage.update((p) => p - 1);
  }
}
