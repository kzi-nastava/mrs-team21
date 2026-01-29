import { Injectable, signal, computed } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  duration: number;
}

/**
 * Global toast notification service.
 * Displays temporary messages to the user with auto-dismiss.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toasts = signal<Toast[]>([]);
  private nextId = 0;

  /** Read-only signal of active toasts */
  readonly activeToasts = this.toasts.asReadonly();

  /** Check if there are any active toasts */
  readonly hasToasts = computed(() => this.toasts().length > 0);

  /**
   * Show a success toast
   * @param message The message to display
   * @param duration How long to show the toast in ms (default: 4000)
   */
  success(message: string, duration = 4000): void {
    this.show(message, 'success', duration);
  }

  /**
   * Show an error toast
   * @param message The message to display
   * @param duration How long to show the toast in ms (default: 5000)
   */
  error(message: string, duration = 5000): void {
    this.show(message, 'error', duration);
  }

  /**
   * Show an info toast
   * @param message The message to display
   * @param duration How long to show the toast in ms (default: 4000)
   */
  info(message: string, duration = 4000): void {
    this.show(message, 'info', duration);
  }

  /**
   * Show a warning toast
   * @param message The message to display
   * @param duration How long to show the toast in ms (default: 4500)
   */
  warning(message: string, duration = 4500): void {
    this.show(message, 'warning', duration);
  }

  /**
   * Dismiss a specific toast by ID
   */
  dismiss(id: number): void {
    this.toasts.update((toasts) => toasts.filter((t) => t.id !== id));
  }

  /**
   * Dismiss all toasts
   */
  dismissAll(): void {
    this.toasts.set([]);
  }

  private show(message: string, type: ToastType, duration: number): void {
    const id = this.nextId++;
    const toast: Toast = { id, message, type, duration };

    this.toasts.update((toasts) => [...toasts, toast]);

    // Auto-dismiss after duration
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }
}
