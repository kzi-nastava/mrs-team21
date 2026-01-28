import { Component, computed, inject, signal, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileApiService } from '../services/profile-api.service';
import {
  ProfileData,
  PersonalInfoForm,
  VehicleInfoForm,
  VehicleCategory,
} from '../models/profile.model';
import { ProfilePhotoUploadComponent } from '../../../shared/components/profile-photo-upload/profile-photo-upload.component';
import { NotificationApiService } from '../services/notification-api.service';
import { UserNotification } from '../models/notification.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ProfilePhotoUploadComponent],
  templateUrl: './profile-page.component.html',
  styleUrls: ['./profile-page.component.scss'],
})
export class ProfilePageComponent implements OnInit {
  private readonly profileService = inject(ProfileApiService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly notificationService = inject(NotificationApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly profile = signal<ProfileData | null>(null);
  readonly profileLoading = signal<boolean>(true);
  readonly profileError = signal<string | null>(null);
  readonly notifications = signal<UserNotification[]>([]);
  readonly notificationsLoading = signal<boolean>(false);
  readonly notificationsError = signal<string | null>(null);

  // Photo upload
  private selectedFile: File | null = null;

  // Edit modal states
  showEditPersonalModal = false;
  showEditVehicleModal = false;
  showSuccessMessage = false;
  showPendingMessage = false;
  successMessage = '';

  // Reactive forms
  personalForm = this.fb.group({
    firstName: [''],
    lastName: [''],
    email: [''],
    phone: [''],
    address: [''],
  });

  vehicleForm = this.fb.group({
    model: [''],
    category: ['Standard'],
    licensePlate: [''],
    seats: [4],
    babySeats: [false],
    petFriendly: [false],
  });

  // Vehicle category options
  vehicleCategories: VehicleCategory[] = ['Standard', 'Luxury', 'Van'];

  readonly fullName = computed(() => {
    const p = this.profile();
    return p ? `${p.firstName} ${p.lastName}`.trim() : '';
  });

  /**
   * Determines if the current user is a driver.
   */
  readonly isDriver = computed(() => this.profile()?.role === 'DRIVER');

  ngOnInit(): void {
    // TODO: Get userId from auth service
    const userId = 1; // Hardcoded for now

    this.profileService
      .getProfile(userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.profile.set(data);
          this.profileLoading.set(false);

          // Load notifications only for non-drivers
          if (data.role !== 'DRIVER') {
            this.loadNotifications();
          }
        },
        error: (error) => {
          console.error('Failed to load profile:', error);
          this.profileError.set('Failed to load profile data');
          this.profileLoading.set(false);
        },
      });
  }

  /**
   * Check if there are any pending changes awaiting admin approval
   */
  readonly hasPendingChanges = computed(() => {
    const p = this.profile();
    return p?.pendingChanges?.some((c) => c.status === 'pending') || false;
  });

  /**
   * Calculate the percentage of daily driving limit used.
   * Maximum allowed is 8 hours in 24 hours.
   */
  getActiveHoursPercentage(): number {
    const p = this.profile();
    const hoursWorked = p?.activeHoursLast24h?.hoursWorked || 0;
    const maxHours = 8;
    return Math.min((hoursWorked / maxHours) * 100, 100);
  }

  /**
   * Get remaining hours the driver can work today.
   */
  getRemainingHours(): number {
    const p = this.profile();
    const hoursWorked = p?.activeHoursLast24h?.hoursWorked || 0;
    return Math.max(8 - hoursWorked, 0);
  }

  /**
   * Get CSS class for vehicle type badge based on category.
   */
  getVehicleTypeClass(category: string): string {
    const normalized = category.toLowerCase();
    if (normalized.includes('luxury')) return 'luxury';
    if (normalized.includes('van')) return 'van';
    return 'standard';
  }

  /**
   * Handle file selection from shared component.
   * File is already cropped to 1:1 by the component.
   */
  onPhotoSelected(file: File): void {
    this.selectedFile = file;
    console.log('Photo selected (auto-cropped to 1:1):', file.name, file.size, 'bytes');

    // For now, use FileReader to convert to base64 data URL for backend storage
    const reader = new FileReader();
    reader.onload = (e) => {
      const dataUrl = e.target?.result as string;
      const p = this.profile();
      if (!p) return;

      // Update profile with new picture
      this.profileService
        .updateProfile(p.id, { profilePictureUrl: dataUrl })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (updatedProfile) => {
            this.profile.set(updatedProfile);
            this.showSuccess('Profile photo updated successfully');
          },
          error: (err) => {
            console.error('Upload failed:', err);
            this.showSuccess('Failed to update profile photo');
          },
        });
    };
    reader.readAsDataURL(file);
  }

  /**
   * Handle image load error - fallback to default avatar.
   */
  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'images/default-avatar.svg';
  }

  /**
   * Navigate to change password page.
   */
  onChangePassword(): void {
    this.router.navigate(['/reset-password', 'change']);
    console.log('Change Password clicked - navigating to reset-password');
  }

  /**
   * Open edit personal information modal
   */
  onEditProfile(): void {
    const p = this.profile();
    if (!p) return;

    // Pre-fill form with current data
    this.personalForm.patchValue({
      firstName: p.firstName,
      lastName: p.lastName,
      email: p.email,
      phone: p.phone,
      address: p.address,
    });
    this.showEditPersonalModal = true;
  }

  /**
   * Close edit personal information modal
   */
  closeEditPersonalModal(): void {
    this.showEditPersonalModal = false;
  }

  /**
   * Save personal information changes
   * - For passengers: saves directly
   * - For drivers: submits change request for admin approval
   */
  savePersonalInfo(): void {
    if (this.isDriver()) {
      // Driver: Submit change request for admin approval
      this.submitDriverChangeRequest('personal', this.personalForm.value as PersonalInfoForm);
    } else {
      // Passenger: Save directly
      this.savePassengerPersonalInfo();
    }
    this.closeEditPersonalModal();
  }

  /**
   * Save passenger personal info directly
   */
  private savePassengerPersonalInfo(): void {
    const values = this.personalForm.value as PersonalInfoForm;
    const p = this.profile();
    if (!p) return;

    // Call backend API to update profile
    this.profileService
      .updateProfile(p.id, {
        name: values.firstName,
        surname: values.lastName,
        email: values.email,
        phone: values.phone,
        address: values.address,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedProfile) => {
          this.profile.set(updatedProfile);
          this.showSuccess('Your profile has been updated successfully!');
          console.log('Passenger profile updated:', values);
        },
        error: (err) => {
          console.error('Failed to update profile:', err);
          this.showSuccess('Failed to update profile');
        },
      });
  }

  /**
   * Submit driver change request for admin approval
   */
  private submitDriverChangeRequest(
    type: 'personal' | 'vehicle',
    changes: PersonalInfoForm | VehicleInfoForm,
  ): void {
    // Create pending change request
    const request = {
      id: `req-${Date.now()}`,
      type,
      requestedAt: new Date(),
      status: 'pending' as const,
      changes,
    };

    // Add to pending changes
    this.profile.update((p) => {
      if (!p) return null;
      const pendingChanges = p.pendingChanges || [];
      return {
        ...p,
        pendingChanges: [...pendingChanges, request],
      };
    });

    // TODO: Call backend API to submit change request
    // this.profileService.submitChangeRequest(request).subscribe(...)

    this.showPending(
      `Your ${type} information change request has been submitted for admin approval.`,
    );
    console.log('Driver change request submitted:', request);
  }

  /**
   * Open edit vehicle information modal
   */
  onEditVehicle(): void {
    const p = this.profile();
    if (!p?.vehicle) return;

    // Pre-fill form with current data
    this.vehicleForm.patchValue({
      model: p.vehicle.model,
      category: p.vehicle.category,
      licensePlate: p.vehicle.licensePlate,
      seats: p.vehicle.seats,
      babySeats: p.vehicle.features.babySeats,
      petFriendly: p.vehicle.features.petFriendly,
    });
    this.showEditVehicleModal = true;
  }

  /**
   * Close edit vehicle information modal
   */
  closeEditVehicleModal(): void {
    this.showEditVehicleModal = false;
  }

  /**
   * Save vehicle information changes
   * Always submits as change request for admin approval (driver only)
   */
  saveVehicleInfo(): void {
    this.submitDriverChangeRequest('vehicle', this.vehicleForm.value as VehicleInfoForm);
    this.closeEditVehicleModal();
  }

  /**
   * Show success message temporarily
   */
  private showSuccess(message: string): void {
    this.successMessage = message;
    this.showSuccessMessage = true;
    setTimeout(() => {
      this.showSuccessMessage = false;
    }, 4000);
  }

  /**
   * Show pending message temporarily
   */
  private showPending(message: string): void {
    this.successMessage = message;
    this.showPendingMessage = true;
    setTimeout(() => {
      this.showPendingMessage = false;
    }, 5000);
  }

  /**
   * Close any notification message
   */
  closeNotification(): void {
    this.showSuccessMessage = false;
    this.showPendingMessage = false;
  }

  openRideHistory(rideId?: number | null): void {
    if (rideId) {
      this.router.navigate(['/ride-history'], { queryParams: { rideId } });
      return;
    }
    this.router.navigate(['/ride-history']);
  }

  formatNotificationDate(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(date));
  }

  private loadNotifications(): void {
    const p = this.profile();
    if (!p) return;

    this.notificationsLoading.set(true);
    this.notificationsError.set(null);
    this.notificationService
      .getUserNotifications(p.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (notifications) => {
          this.notifications.set(notifications);
          this.notificationsLoading.set(false);
        },
        error: (error) => {
          console.error('Failed to load notifications', error);
          this.notificationsError.set('Unable to load notifications.');
          this.notificationsLoading.set(false);
        },
      });
  }
}
