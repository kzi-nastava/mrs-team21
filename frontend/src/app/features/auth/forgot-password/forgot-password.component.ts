import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CommonModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent implements OnInit {
  forgotPasswordForm!: FormGroup;
  submitted = false;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  get f() {
    return this.forgotPasswordForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.forgotPasswordForm.invalid) {
      return;
    }

    // TODO: Implement password reset logic in KT2
    console.log('Password reset requested for:', this.forgotPasswordForm.value.email);
  }
}
