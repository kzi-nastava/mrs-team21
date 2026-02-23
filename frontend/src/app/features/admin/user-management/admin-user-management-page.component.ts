import {
  Component,
  OnInit,
  signal,
  inject,
  DestroyRef,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  UserManagementApiService,
  UserResponse,
  UserRole,
} from '../services/user-management-api.service';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-admin-user-management-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-user-management-page.component.html',
  styleUrl: './admin-user-management-page.component.scss',
})
export class AdminUserManagementPageComponent implements OnInit {
  private readonly userManagementApi = inject(UserManagementApiService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  users = signal<UserResponse[]>([]);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);
  roleFilter = signal<UserRole | ''>('');
  isLoading = signal<boolean>(false);
  actionInProgress = signal<number | null>(null);

  roleFilterOptions: { value: '' | UserRole; label: string }[] = [
    { value: '', label: 'All' },
    { value: 'PASSENGER', label: 'Passenger' },
    { value: 'DRIVER', label: 'Driver' },
  ];

  hasPreviousPage = computed(
    () => this.currentPage() > 0 && !this.isLoading()
  );
  hasNextPage = computed(
    () =>
      this.currentPage() < this.totalPages() - 1 &&
      this.totalPages() > 0 &&
      !this.isLoading()
  );

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    const page = this.currentPage();
    const size = this.pageSize();
    const role = this.roleFilter() || undefined;

    this.userManagementApi
      .getUsers(page, size, 'email,asc', role)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.users.set(response.content ?? []);
          this.totalElements.set(response.totalElements ?? 0);
          const total = response.totalElements ?? 0;
          const size = response.size ?? this.pageSize();
          this.totalPages.set(response.totalPages ?? Math.max(1, Math.ceil(total / size)));
        },
        error: (err: { status?: number; error?: { message?: string }; message?: string }) => {
          const msg =
            err?.error?.message ||
            (err?.status === 404
              ? 'User list endpoint not found. Is the backend running with the latest code?'
              : err?.status === 403
                ? 'Access denied. You must be logged in as admin.'
                : 'Failed to load users. Please try again.');
          this.toastService.error(msg);
          this.users.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
        },
      });
  }

  onRoleFilterChange(value: string): void {
    const role: '' | UserRole =
      value === 'PASSENGER' || value === 'DRIVER' ? value : '';
    this.roleFilter.set(role);
    this.currentPage.set(0);
    this.loadUsers();
  }

  blockUser(user: UserResponse): void {
    if (user.blocked) return;
    this.actionInProgress.set(user.id);
    this.userManagementApi
      .blockUser(user.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionInProgress.set(null)),
      )
      .subscribe({
        next: () => {
          this.toastService.success('User blocked.');
          this.loadUsers();
        },
        error: () => {
          this.toastService.error('Failed to block user.');
        },
      });
  }

  unblockUser(user: UserResponse): void {
    if (!user.blocked) return;
    this.actionInProgress.set(user.id);
    this.userManagementApi
      .unblockUser(user.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionInProgress.set(null)),
      )
      .subscribe({
        next: () => {
          this.toastService.success('User unblocked.');
          this.loadUsers();
        },
        error: () => {
          this.toastService.error('Failed to unblock user.');
        },
      });
  }

  goToPreviousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((p) => p - 1);
      this.loadUsers();
    }
  }

  goToNextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update((p) => p + 1);
      this.loadUsers();
    }
  }

  displayRole(role: UserRole): string {
    return role === 'DRIVER' ? 'Driver' : role === 'PASSENGER' ? 'Passenger' : role;
  }
}
