import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VehicleTypePricingService, VehicleTypeResponse } from '../services/vehicle-type-pricing.service';
import { ToastService } from '../../../shared/services/toast.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-vehicle-type-pricing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './vehicle-type-pricing.component.html',
  styleUrl: './vehicle-type-pricing.component.scss',
})
export class VehicleTypePricingComponent implements OnInit {
  private readonly pricingService = inject(VehicleTypePricingService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

  vehicleTypes: VehicleTypeResponse[] = [];
  loading = true;
  savingId: number | null = null;

  /** Form group keyed by vehicle type id */
  forms: Map<number, FormGroup> = new Map();

  ngOnInit(): void {
    this.loadVehicleTypes();
  }

  private loadVehicleTypes(): void {
    this.loading = true;
    this.pricingService
      .getAll()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (list) => {
          this.vehicleTypes = Array.isArray(list) ? list : [];
          this.buildForms();
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.toastService.error('Failed to load vehicle type pricing.');
        },
      });
  }

  private buildForms(): void {
    this.forms.clear();
    for (const vt of this.vehicleTypes) {
      const group = this.fb.group({
        startPrice: [
          this.toNumber(vt.startPrice),
          [Validators.required, Validators.min(0)],
        ],
        pricePerKm: [
          this.toNumber(vt.pricePerKm),
          [Validators.required, Validators.min(0)],
        ],
      });
      this.forms.set(vt.id, group);
    }
  }

  private toNumber(value: number | string | undefined): number {
    if (value == null) return 0;
    const n = typeof value === 'string' ? parseFloat(value) : value;
    return Number.isFinite(n) ? n : 0;
  }

  getForm(id: number): FormGroup | undefined {
    return this.forms.get(id);
  }

  save(id: number): void {
    const form = this.forms.get(id);
    if (!form || form.invalid || this.savingId !== null) return;

    const startPrice = Number(form.get('startPrice')?.value);
    const pricePerKm = Number(form.get('pricePerKm')?.value);
    if (startPrice < 0 || pricePerKm < 0) return;

    this.savingId = id;
    this.pricingService
      .update(id, { startPrice, pricePerKm })
      .pipe(finalize(() => (this.savingId = null)))
      .subscribe({
        next: (updated) => {
          this.toastService.success(`Pricing for ${updated.name} updated.`);
          const index = this.vehicleTypes.findIndex((v) => v.id === id);
          if (index !== -1) {
            this.vehicleTypes = [
              ...this.vehicleTypes.slice(0, index),
              updated,
              ...this.vehicleTypes.slice(index + 1),
            ];
            const newForm = this.fb.group({
              startPrice: [
                this.toNumber(updated.startPrice),
                [Validators.required, Validators.min(0)],
              ],
              pricePerKm: [
                this.toNumber(updated.pricePerKm),
                [Validators.required, Validators.min(0)],
              ],
            });
            this.forms.set(id, newForm);
          }
          this.savingId = null;
          this.cdr.markForCheck();
        },
        error: (err) => {
          const msg =
            err?.error?.message ||
            (Array.isArray(err?.error?.errors)
              ? err.error.errors.map((e: { defaultMessage?: string }) => e.defaultMessage).join(', ')
              : 'Failed to update pricing.');
          this.toastService.error(msg);
          this.savingId = null;
          this.cdr.markForCheck();
        },
      });
  }
}
