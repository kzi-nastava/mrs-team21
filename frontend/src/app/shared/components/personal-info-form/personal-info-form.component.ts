import { Component, Input, Output, EventEmitter, input, output } from '@angular/core';
import { ReactiveFormsModule, FormGroup } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-personal-info-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './personal-info-form.component.html',
  styleUrl: './personal-info-form.component.scss',
})
export class PersonalInfoFormComponent {
  // need to mutate it
  @Input() form!: FormGroup;
  
  // read-only configuration
  submitted = input(false);
  showPasswordFields = input(true);
  emailHint = input('');
  showPassword = input(false);
  showConfirmPassword = input(false);
  
  togglePassword = output<void>();
  toggleConfirmPassword = output<void>();

  get f() {
    return this.form.controls;
  }

  onTogglePassword(): void {
    this.togglePassword.emit();
  }

  onToggleConfirmPassword(): void {
    this.toggleConfirmPassword.emit();
  }
}