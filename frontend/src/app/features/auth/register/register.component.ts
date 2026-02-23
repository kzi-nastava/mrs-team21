import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PersonalInfoFormComponent } from '../../../shared/components/personal-info-form/personal-info-form.component';
import { ProfilePhotoUploadComponent } from '../../../shared/components/profile-photo-upload/profile-photo-upload.component';
import { RegisterService, PassengerRegisterRequest } from '../services/register.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    CommonModule,
    PersonalInfoFormComponent,
    ProfilePhotoUploadComponent,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private registerService = inject(RegisterService);
  private router = inject(Router);

  registerForm!: FormGroup;
  submitted = false;
  showPassword = false;
  showConfirmPassword = false;
  selectedPhotoFile: File | null = null;

  ngOnInit(): void {
    this.registerForm = this.fb.group(
      {
        firstName: ['', [Validators.required, Validators.minLength(2)]],
        lastName: ['', [Validators.required, Validators.minLength(2)]],
        email: ['', [Validators.required, Validators.email]],
        countryCode: ['+381', [Validators.required]],
        phone: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]],
        address: ['', [Validators.required, Validators.minLength(5)]],
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(6),
            Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/),
          ],
        ],
        confirmPassword: ['', [Validators.required]],
        agreeToTerms: [false, [Validators.requiredTrue]],
      },
      { validators: this.passwordMatchValidator },
    );
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  get f() {
    return this.registerForm.controls;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onPhotoSelected(file: File): void {
    this.selectedPhotoFile = file;
    console.log('Photo selected (auto-cropped to 1:1):', file.name, file.size, 'bytes');
  }

  private submitWithPayload(payload: PassengerRegisterRequest): void {
    this.registerService.register(payload).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);
        this.router.navigate(['/login']);
      },
      error: (error) => {
        console.error('Registration failed:', error);
      },
    });
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.registerForm.invalid) {
      return;
    }

    const formValue = this.registerForm.value;
    const basePayload: Omit<PassengerRegisterRequest, 'profilePictureUrl'> = {
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      email: formValue.email,
      phoneNumber: formValue.countryCode + formValue.phone,
      address: formValue.address,
      password: formValue.password,
      confirmPassword: formValue.confirmPassword,
    };

    const file = this.selectedPhotoFile;
    if (!file) {
      this.submitWithPayload({ ...basePayload, profilePictureUrl: undefined });
      return;
    }

    this.registerService.uploadProfilePicture(file).subscribe({
      next: (res) => {
        this.submitWithPayload({ ...basePayload, profilePictureUrl: res.url });
      },
      error: (err) => {
        console.error('Profile picture upload failed:', err);
        this.submitWithPayload({ ...basePayload, profilePictureUrl: undefined });
      },
    });
  }
}
