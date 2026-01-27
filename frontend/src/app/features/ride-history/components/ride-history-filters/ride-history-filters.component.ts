import { Component, output, input, ChangeDetectionStrategy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/**
 * Reusable ride history filter component.
 * Emits filter changes without managing state internally.
 */
@Component({
  selector: 'app-ride-history-filters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ride-history-filters.component.html',
  styleUrl: './ride-history-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RideHistoryFiltersComponent {
  startDate = input<string>('');
  endDate = input<string>('');
  resultsCount = input<number>(0);
  showDateFilters = input<boolean>(true);

  filterChanged = output<{ startDate: string; endDate: string }>();
  filterCleared = output<void>();

  localStartDate: string = '';
  localEndDate: string = '';

  constructor() {
    // Keep local fields in sync with input signals
    effect(() => {
      this.localStartDate = this.startDate();
      this.localEndDate = this.endDate();
    });
  }

  onDateChange(): void {
    this.filterChanged.emit({ startDate: this.localStartDate, endDate: this.localEndDate });
  }

  onClearFilter(): void {
    this.localStartDate = '';
    this.localEndDate = '';
    this.filterCleared.emit();
  }
}
