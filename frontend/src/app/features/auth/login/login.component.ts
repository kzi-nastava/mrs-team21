import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LoginService, LoginRequest } from '../services/login.service';
import { AuthService } from '../../../shared/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  logoSrc = 'images/logo/logo-white-v2.svg';
  loginForm!: FormGroup;
  submitted = false;

  private fb = inject(FormBuilder);
  private loginService = inject(LoginService);
  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(6),
          Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/),
        ],
      ],
      rememberMe: [false],
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.loginForm.invalid) {
      return;
    }

    const formValue = this.loginForm.value;
    const payload: LoginRequest = {
      email: formValue.email,
      password: formValue.password,
    };

    this.loginService.login(payload).subscribe({
      next: (response) => {
        console.log('Login successful:', response);
        this.authService.setToken(response.token);
        const defaultRoute = this.authService.getDefaultRouteForRole(response.role);
        this.router.navigate([defaultRoute]);
      },
      error: (error) => {
        console.error('Login failed:', error);
        console.error('Error details:', error.error);
        alert(
          `Login failed: ${error.error?.message || error.message || 'Invalid email or password'}`,
        );
      },
    });
  }
}
