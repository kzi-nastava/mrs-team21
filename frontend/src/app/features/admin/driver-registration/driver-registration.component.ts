import { Component, OnInit } from '@angular/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { NgIf, CommonModule } from '@angular/common';
import { PersonalInfoFormComponent } from '../../../shared/components/personal-info-form/personal-info-form.component';

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
  imports: [ReactiveFormsModule, NgIf, CommonModule, PersonalInfoFormComponent],
  templateUrl: './driver-registration.component.html',
  styleUrl: './driver-registration.component.scss',
})
export class DriverRegistrationComponent implements OnInit {
  currentStep = 1;
  totalSteps = 2;
  
  driverForm!: FormGroup;
  vehicleForm!: FormGroup;
  
  driverSubmitted = false;
  vehicleSubmitted = false;
  
  showSuccessMessage = false;
  isSubmitting = false;

  vehicleCategories: VehicleCategory[] = ['Standard', 'Luxury', 'Van'];

  constructor(private fb: FormBuilder) {}

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

    const registrationPayload = {
      driver: {
        firstName: driverData.firstName,
        lastName: driverData.lastName,
        email: driverData.email,
        phone: driverData.countryCode + driverData.phone,
        address: driverData.address,
      },
      vehicle: {
        model: vehicleData.model,
        category: vehicleData.category,
        licensePlate: vehicleData.licensePlate.toUpperCase(),
        seats: vehicleData.seats,
        features: {
          babySeats: vehicleData.babySeats,
          petFriendly: vehicleData.petFriendly,
        },
      },
    };

    // TODO: Implement API call to create driver
    // The backend will:
    // 1. Create the driver account with a generated password
    // 2. Send an email with password reset link to the driver
    console.log('Driver registration submitted:', registrationPayload);

    // Simulate API call
    setTimeout(() => {
      this.isSubmitting = false;
      this.showSuccessMessage = true;
    }, 1000);
  }

  addAnotherDriver(): void {
    this.showSuccessMessage = false;
    this.currentStep = 1;
    this.driverSubmitted = false;
    this.vehicleSubmitted = false;
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
