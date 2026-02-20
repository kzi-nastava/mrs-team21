import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
  PanicApiService,
  PanicEventsPageResponse,
} from '../../services/panic-api.service';

const POLL_INTERVAL_MS = 2000;

const PAGE_SIZE_OPTIONS = [10, 25, 50];

function formatDate(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    dateStyle: 'short',
    timeStyle: 'short',
  });
}

@Component({
  selector: 'app-admin-panic-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-panic-page.component.html',
  styleUrl: './admin-panic-page.component.scss',
})
export class AdminPanicPageComponent implements OnInit {
  private readonly panicApi = inject(PanicApiService);
  private readonly destroyRef = inject(DestroyRef);

  pageData = signal<PanicEventsPageResponse | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  currentPage = signal(0);
  pageSize = signal(10);
  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  events = computed(() => this.pageData()?.content ?? []);
  totalElements = computed(() => this.pageData()?.totalElements ?? 0);
  totalPages = computed(() => this.pageData()?.totalPages ?? 0);

  hasPrev = computed(() => this.currentPage() > 0);
  hasNext = computed(
    () => this.currentPage() < this.totalPages() - 1 && this.totalPages() > 0
  );

  setPageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.load();
  }

  prevPage(): void {
    if (this.hasPrev()) {
      this.currentPage.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.hasNext()) {
      this.currentPage.update((p) => p + 1);
      this.load();
    }
  }

  formatEventDate(iso: string): string {
    return formatDate(iso);
  }

  ngOnInit(): void {
    this.load();
    this.startPolling();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.panicApi
      .getPanicEventsPage(
        this.currentPage(),
        this.pageSize(),
        'createdAt,desc'
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.pageData.set(res);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(
            err.error?.message ?? 'Failed to load panic notifications.'
          );
          this.pageData.set(null);
          this.loading.set(false);
        },
      });
  }

  private startPolling(): void {
    interval(POLL_INTERVAL_MS)
      .pipe(
        switchMap(() => this.panicApi.getPanicEventsPage(
          this.currentPage(),
          this.pageSize(),
          'createdAt,desc'
        )),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (res) => {
          this.pageData.set(res);
          this.error.set(null);
        },
        error: (err) => {
          this.error.set(
            err.error?.message ?? 'Failed to load panic notifications.'
          );
        },
      });
  }
}
