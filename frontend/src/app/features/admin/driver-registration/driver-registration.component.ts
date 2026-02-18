import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PersonalInfoFormComponent } from '../../../shared/components/personal-info-form/personal-info-form.component';
import { ProfilePhotoUploadComponent } from '../../../shared/components/profile-photo-upload/profile-photo-upload.component';
import { DriverRegistrationService } from '../services/driver-registration.service';
import { finalize } from 'rxjs/operators';
import { ToastService } from '../../../shared/services/toast.service';

type VehicleCategory = 'Standard' | 'Luxury' | 'Van';

interface DriverFormData {
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  phone: string;
  address: string;
}

interface VehicleFormData {
  model: string;
  category: VehicleCategory;
  licensePlate: string;
  seats: number;
  babySeats: boolean;
  petFriendly: boolean;
}

@Component({
  selector: 'app-driver-registration',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    PersonalInfoFormComponent,
    ProfilePhotoUploadComponent,
  ],
  templateUrl: './driver-registration.component.html',
  styleUrl: './driver-registration.component.scss',
})
export class DriverRegistrationComponent implements OnInit {
  private fb = inject(FormBuilder);
  private driverRegistrationService = inject(DriverRegistrationService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  currentStep = 1;
  totalSteps = 2;

  driverForm!: FormGroup;
  vehicleForm!: FormGroup;

  driverSubmitted = false;
  vehicleSubmitted = false;

  showSuccessMessage = false;
  isSubmitting = false;

  selectedPhotoFile: File | null = null;

  vehicleCategories: VehicleCategory[] = ['Standard', 'Luxury', 'Van'];

  ngOnInit(): void {
    this.initDriverForm();
    this.initVehicleForm();
  }

  private initDriverForm(): void {
    this.driverForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      countryCode: ['+381', [Validators.required]],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]],
      address: ['', [Validators.required, Validators.minLength(5)]],
    });
  }

  private initVehicleForm(): void {
    this.vehicleForm = this.fb.group({
      model: ['', [Validators.required, Validators.minLength(2)]],
      category: ['Standard', [Validators.required]],
      licensePlate: ['', [Validators.required, Validators.pattern(/^[A-Z0-9\-\s]{4,12}$/i)]],
      seats: [4, [Validators.required, Validators.min(1), Validators.max(8)]],
      babySeats: [false],
      petFriendly: [false],
    });
  }

  get df() {
    return this.driverForm.controls;
  }

  get vf() {
    return this.vehicleForm.controls;
  }

  onPhotoSelected(file: File): void {
    this.selectedPhotoFile = file;
    console.log('Driver photo selected (auto-cropped to 1:1):', file.name, file.size, 'bytes');
    // TODO: Upload to backend storage and get URL
    // this.uploadService.uploadProfilePhoto(file).subscribe(url => ...);
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      this.driverSubmitted = true;
      if (this.driverForm.invalid) {
        return;
      }
    }

    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      // Reset validation state when going back
      if (this.currentStep === 1) {
        this.driverSubmitted = false;
      }
    }
  }

  goToStep(step: number): void {
    // Only allow going back or to completed steps
    if (step < this.currentStep) {
      // Reset validation state when navigating back
      if (step === 1) {
        this.driverSubmitted = false;
      }
      this.currentStep = step;
    } else if (step === 2 && this.driverForm.valid) {
      this.currentStep = step;
    }
  }

  onSubmit(): void {
    this.vehicleSubmitted = true;

    if (this.vehicleForm.invalid) {
      return;
    }

    if (this.driverForm.invalid) {
      this.currentStep = 1;
      this.driverSubmitted = true;
      return;
    }

    this.isSubmitting = true;

    // Prepare the data
    const driverData: DriverFormData = this.driverForm.value;
    const vehicleData: VehicleFormData = this.vehicleForm.value;

    const registrationRequest = {
      name: driverData.firstName,
      surname: driverData.lastName,
      email: driverData.email,
      phone: driverData.countryCode + driverData.phone,
      address: driverData.address,
      vehicleTypeId: this.driverRegistrationService.mapCategoryToTypeId(vehicleData.category),
      vehicleModel: vehicleData.model,
      vehicleLicensePlate: vehicleData.licensePlate.toUpperCase(),
      vehicleNumSeats: vehicleData.seats,
      vehicleBabyFriendly: vehicleData.babySeats,
      vehiclePetFriendly: vehicleData.petFriendly,
    };

    this.driverRegistrationService
      .registerDriver(registrationRequest)
      // Always stop the spinner, even if an interceptor completes the stream without next/error.
      .pipe(
        finalize(() => {
          this.isSubmitting = false;
        }),
      )
      .subscribe({
      next: (response) => {
        console.log('Driver registered successfully:', response);
        // UX: immediately reset the form to allow registering the next driver.
        // Admin should not get "stuck" on a separate success screen.
        this.toastService.success('Driver created. Activation email sent.');
        this.addAnotherDriver();
      },
      error: (error) => {
        console.error('Error registering driver:', error);
        // TODO: Show error message to user
        alert(error.error?.message || 'Failed to register driver. Please try again.');
      },
    });
  }

  addAnotherDriver(): void {
    this.showSuccessMessage = false;
    this.currentStep = 1;
    this.driverSubmitted = false;
    this.vehicleSubmitted = false;
    this.selectedPhotoFile = null;
    this.driverForm.reset({
      countryCode: '+381',
    });
    this.vehicleForm.reset({
      category: 'Standard',
      seats: 4,
      babySeats: false,
      petFriendly: false,
    });
  }

  getStepStatus(step: number): string {
    if (step < this.currentStep) {
      return 'completed';
    } else if (step === this.currentStep) {
      return 'active';
    }
    return 'pending';
  }

  decrementSeats(): void {
    const currentSeats = this.vehicleForm.get('seats')?.value || 4;
    if (currentSeats > 1) {
      this.vehicleForm.patchValue({ seats: currentSeats - 1 });
    }
  }

  incrementSeats(): void {
    const currentSeats = this.vehicleForm.get('seats')?.value || 4;
    if (currentSeats < 8) {
      this.vehicleForm.patchValue({ seats: currentSeats + 1 });
    }
  }
}
