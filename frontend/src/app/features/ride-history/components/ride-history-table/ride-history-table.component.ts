import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../models';

export interface TableColumnConfig {
  key: string;
  label: string;
  visible: boolean;
}

export type SortField = 'dateTime' | 'driver' | 'passengers' | 'route' | 'status' | 'earnings';
export type SortOrder = 'asc' | 'desc';

export interface SortState {
  field: SortField | null;
  order: SortOrder;
}

/**
 * Reusable ride history table component.
 * Presentational only - no role-specific logic.
 * Configuration via inputs controls visibility and behavior.
 */
@Component({
  selector: 'app-ride-history-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-history-table.component.html',
  styleUrl: './ride-history-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RideHistoryTableComponent {
  rides = input<Ride[]>([]);
  columns = input<TableColumnConfig[]>([]);
  showEarnings = input(false);
  showPassengers = input(true);
  showFavoriteButton = input(false);
  emptyMessage = input('No rides found.');

  rideSelected = output<Ride>();
  sortChanged = output<SortState>();
  favoriteToggled = output<Ride>();

  sortState: SortState = { field: null, order: 'asc' };

  formatDate(date: Date | null): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(date);
  }

  formatTime(date: Date | null): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }

  formatDateTimeRange(start: Date, end: Date | null): string {
    if (!end) return `${this.formatTime(start)} - —`;
    return `${this.formatTime(start)} - ${this.formatTime(end)}`;
  }

  formatCurrency(amount: number): string {
    if (amount === 0) return '—';
    return new Intl.NumberFormat('sr-RS', {
      style: 'currency',
      currency: 'RSD',
      minimumFractionDigits: 0,
    }).format(amount);
  }

  getVehicleTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      STANDARD: 'Standard',
      LUXURY: 'Luxury',
      VAN: 'Van',
    };
    return labels[type] || type;
  }

  getPassengerInitials(passenger: { firstName: string; lastName: string }): string {
    const first = passenger.firstName?.[0] || '';
    const last = passenger.lastName?.[0] || '';
    return (first + last).toUpperCase() || 'U';
  }

  getDriverInitials(driver: { firstName: string; lastName: string } | undefined): string {
    if (!driver) return 'D';
    const first = driver.firstName?.[0] || '';
    const last = driver.lastName?.[0] || '';
    return (first + last).toUpperCase() || 'D';
  }

  getDriverName(driver: { firstName: string; lastName: string } | undefined): string {
    if (!driver) return 'Unknown';
    return `${driver.firstName} ${driver.lastName}`;
  }

  getPassengerDisplayCount(count: number): string {
    return count === 1 ? '1 passenger' : `${count} passengers`;
  }

  calculateDuration(start: Date, end: Date | null): string {
    if (!end) return '—';
    const diffMs = end.getTime() - start.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    return `${diffMins} min`;
  }

  calculateDistance(_origin: string, _destination: string): string {
    // Mock distance calculation - in real app this would use map API
    return '3.2 km';
  }

  selectRide(ride: Ride): void {
    this.rideSelected.emit(ride);
  }

  isColumnVisible(columnKey: string): boolean {
    return this.columns().find((c: TableColumnConfig) => c.key === columnKey)?.visible ?? false;
  }

  onHeaderClick(field: SortField): void {
    if (this.sortState.field === field) {
      this.sortState.order = this.sortState.order === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortState.field = field;
      this.sortState.order = 'asc';
    }
    this.sortChanged.emit(this.sortState);
  }

  getSortIcon(field: SortField): string {
    if (this.sortState.field !== field) return '⇅';
    return this.sortState.order === 'asc' ? '↑' : '↓';
  }

  isSortable(field: SortField): boolean {
    const sortableFields: SortField[] = ['dateTime', 'driver', 'passengers', 'route', 'status', 'earnings'];
    return sortableFields.includes(field);
  }
}
