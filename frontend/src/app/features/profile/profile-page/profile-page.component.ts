import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileMockService } from '../services/profile-mock.service';
import { ProfileData, PersonalInfoForm, VehicleInfoForm, VehicleCategory } from '../models/profile.model';
import { ProfilePhotoUploadComponent } from '../../../shared/components/profile-photo-upload/profile-photo-upload.component';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ProfilePhotoUploadComponent],
  templateUrl: './profile-page.component.html',
  styleUrls: ['./profile-page.component.scss'],
})
export class ProfilePageComponent {
  private readonly profileService = inject(ProfileMockService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly profile: ProfileData = this.profileService.getProfile();

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

  readonly fullName = computed(() => `${this.profile.firstName} ${this.profile.lastName}`.trim());

  /**
   * Determines if the current user is a driver.
   * 
   * TODO: Connect to actual auth/user service
   * Example integration:
   * ```
   * private readonly authService = inject(AuthService);
   * readonly isDriver = computed(() => this.authService.currentUser()?.role === 'DRIVER');
   * ```
   * 
   * The role should come from:
   * 1. JWT token claims after login
   * 2. User service that fetches user data
   * 3. Auth guard that determines access
   */
  readonly isDriver = computed(() => this.profile.role === 'DRIVER');

  /**
   * Check if there are any pending changes awaiting admin approval
   */
  readonly hasPendingChanges = computed(() => 
    this.profile.pendingChanges?.some(c => c.status === 'pending') || false
  );

  /**
   * Calculate the percentage of daily driving limit used.
   * Maximum allowed is 8 hours in 24 hours.
   */
  getActiveHoursPercentage(): number {
    const hoursWorked = this.profile.activeHoursLast24h?.hoursWorked || 0;
    const maxHours = 8;
    return Math.min((hoursWorked / maxHours) * 100, 100);
  }

  /**
   * Get remaining hours the driver can work today.
   */
  getRemainingHours(): number {
    const hoursWorked = this.profile.activeHoursLast24h?.hoursWorked || 0;
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
    
    // TODO: Upload to backend immediately
    // this.uploadService.uploadProfilePhoto(file).subscribe({
    //   next: (url) => {
    //     this.userService.updateProfile({ profilePictureUrl: url }).subscribe({
    //       next: () => {
    //         this.profile.avatarUrl = url;
    //         this.showSuccessMessage('Profile photo updated successfully');
    //       }
    //     });
    //   },
    //   error: (err) => console.error('Upload failed:', err)
    // });
    
    // For now, update preview only (backend integration pending)
    this.profile.avatarUrl = URL.createObjectURL(file);
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
    // Pre-fill form with current data
    this.personalForm.patchValue({
      firstName: this.profile.firstName,
      lastName: this.profile.lastName,
      email: this.profile.email,
      phone: this.profile.phone,
      address: this.profile.address,
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
    // Update profile data locally
    const values = this.personalForm.value as PersonalInfoForm;
    this.profile.firstName = values.firstName;
    this.profile.lastName = values.lastName;
    this.profile.email = values.email;
    this.profile.phone = values.phone;
    this.profile.address = values.address;

    // TODO: Call backend API
    // this.profileService.updateProfile(this.editPersonalForm).subscribe(...)

    this.showSuccess('Your profile has been updated successfully!');
    console.log('Passenger profile updated:', values);
  }

  /**
   * Submit driver change request for admin approval
   */
  private submitDriverChangeRequest(type: 'personal' | 'vehicle', changes: PersonalInfoForm | VehicleInfoForm): void {
    // Create pending change request
    const request = {
      id: `req-${Date.now()}`,
      type,
      requestedAt: new Date(),
      status: 'pending' as const,
      changes,
    };

    // Add to pending changes
    if (!this.profile.pendingChanges) {
      this.profile.pendingChanges = [];
    }
    this.profile.pendingChanges.push(request);

    // TODO: Call backend API to submit change request
    // this.profileService.submitChangeRequest(request).subscribe(...)

    this.showPending(`Your ${type} information change request has been submitted for admin approval.`);
    console.log('Driver change request submitted:', request);
  }

  /**
   * Open edit vehicle information modal
   */
  onEditVehicle(): void {
    if (this.profile.vehicle) {
      // Pre-fill form with current data
      this.vehicleForm.patchValue({
        model: this.profile.vehicle.model,
        category: this.profile.vehicle.category,
        licensePlate: this.profile.vehicle.licensePlate,
        seats: this.profile.vehicle.seats,
        babySeats: this.profile.vehicle.features.babySeats,
        petFriendly: this.profile.vehicle.features.petFriendly,
      });
    }
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
}
