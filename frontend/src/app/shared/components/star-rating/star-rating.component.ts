import {
  Component,
  input,
  output,
  signal,
  forwardRef,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
} from '@angular/forms';

/**
 * Reusable star rating component with form integration.
 * Supports hover preview, keyboard navigation, and accessibility.
 *
 * Usage:
 * - Standalone: <app-star-rating [rating]="3" (ratingChange)="onRate($event)" />
 * - Reactive Forms: <app-star-rating formControlName="rating" />
 */
@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './star-rating.component.html',
  styleUrl: './star-rating.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StarRatingComponent),
      multi: true,
    },
  ],
})
export class StarRatingComponent implements ControlValueAccessor {
  /** Initial rating value (1-5) */
  rating = input<number>(0);

  /** Whether the component is readonly (display only) */
  readonly = input<boolean>(false);

  /** Size of the stars */
  size = input<'sm' | 'md' | 'lg'>('md');

  /** Label for accessibility */
  label = input<string>('Rating');

  /** Emits when rating changes */
  ratingChange = output<number>();

  /** Internal value for form integration */
  protected value = signal<number>(0);

  /** Currently hovered star (0 = none) */
  protected hoverRating = signal<number>(0);

  /** Whether the component is disabled (from forms) */
  protected disabled = signal<boolean>(false);

  readonly stars = [1, 2, 3, 4, 5];

  private onChange: (value: number) => void = () => {};
  private onTouched: () => void = () => {};

  // ControlValueAccessor implementation
  writeValue(value: number): void {
    this.value.set(value || 0);
  }

  registerOnChange(fn: (value: number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  // Interaction methods
  onStarClick(star: number): void {
    if (this.readonly() || this.disabled()) return;

    this.value.set(star);
    this.onChange(star);
    this.ratingChange.emit(star);
    this.onTouched();
  }

  onStarHover(star: number): void {
    if (this.readonly() || this.disabled()) return;
    this.hoverRating.set(star);
  }

  onMouseLeave(): void {
    this.hoverRating.set(0);
  }

  onKeyDown(event: KeyboardEvent, star: number): void {
    if (this.readonly() || this.disabled()) return;

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.onStarClick(star);
    } else if (event.key === 'ArrowRight' && star < 5) {
      event.preventDefault();
      this.onStarClick(star + 1);
    } else if (event.key === 'ArrowLeft' && star > 1) {
      event.preventDefault();
      this.onStarClick(star - 1);
    }
  }

  /** Get the display rating (hover takes precedence) */
  getDisplayRating(): number {
    return this.hoverRating() || this.value() || this.rating();
  }

  /** Check if a star should be filled */
  isStarFilled(star: number): boolean {
    return star <= this.getDisplayRating();
  }

  /** Check if currently hovering (for animation) */
  isHovering(): boolean {
    return this.hoverRating() > 0;
  }
}
