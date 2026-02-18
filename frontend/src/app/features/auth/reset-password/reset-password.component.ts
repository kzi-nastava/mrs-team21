import { Component, OnInit } from '@angular/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { NgClass, CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, NgClass, CommonModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm!: FormGroup;
  submitted = false;
  showPassword = false;
  showConfirmPassword = false;
  isSubmitting = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;
  token: string | null = null;
  flow: 'password-reset' | 'driver-activation' = 'password-reset';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token');
    this.flow = this.route.snapshot.data['flow'] === 'driver-activation' ? 'driver-activation' : 'password-reset';

    this.resetPasswordForm = this.fb.group(
      {
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(6),
            Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/),
          ],
        ],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );

    if (!this.token) {
      this.errorMessage = 'Invalid password link.';
    }
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
    return this.resetPasswordForm.controls;
  }

  get password(): string {
    return this.resetPasswordForm.get('password')?.value || '';
  }

  get hasMinLength(): boolean {
    return this.password.length >= 6;
  }

  get hasUppercase(): boolean {
    return /[A-Z]/.test(this.password);
  }

  get hasNumber(): boolean {
    return /\d/.test(this.password);
  }

  get passwordsMatch(): boolean {
    const confirmPassword = this.resetPasswordForm.get('confirmPassword')?.value;
    return this.password === confirmPassword && confirmPassword.length > 0;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;
    this.successMessage = null;

    if (this.resetPasswordForm.invalid || !this.token) {
      return;
    }

    this.isSubmitting = true;
    const password = this.resetPasswordForm.value.password as string;

    const request$ =
      this.flow === 'driver-activation'
        ? this.http.put<void>(`${environment.apiBaseUrl}/activation/${this.token}/set-password`, { password })
        : this.http.post<void>(`${environment.apiBaseUrl}/auth/reset-password/${this.token}`, password, {
            headers: { 'Content-Type': 'text/plain' },
          });

    request$.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage =
          this.flow === 'driver-activation'
            ? 'Password set successfully. Redirecting to login...'
            : 'Password reset successful. Redirecting to login...';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error?.error?.message || 'Failed to update password. Please try again.';
      },
    });
  }
}
