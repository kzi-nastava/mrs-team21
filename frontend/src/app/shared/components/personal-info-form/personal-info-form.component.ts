import { Component, Input, Output, EventEmitter } from '@angular/core';
import {
  ReactiveFormsModule,
  FormGroup,
} from '@angular/forms';
import { NgIf, CommonModule } from '@angular/common';

@Component({
  selector: 'app-personal-info-form',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf, CommonModule],
  templateUrl: './personal-info-form.component.html',
  styleUrl: './personal-info-form.component.scss',
})
export class PersonalInfoFormComponent {
  @Input() form!: FormGroup;
  @Input() submitted = false;
  @Input() showPasswordFields = true;
  @Input() emailHint = '';
  
  @Output() togglePassword = new EventEmitter<void>();
  @Output() toggleConfirmPassword = new EventEmitter<void>();

  @Input() showPassword = false;
  @Input() showConfirmPassword = false;

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
