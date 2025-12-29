import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProfileMockService } from '../services/profile-mock.service';
import { ProfileData } from '../models/profile.model';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile-page.component.html',
  styleUrls: ['./profile-page.component.scss'],
})
export class ProfilePageComponent {
  private readonly profileService = inject(ProfileMockService);

  readonly profile: ProfileData = this.profileService.getProfile();

  readonly fullName = computed(() => `${this.profile.firstName} ${this.profile.lastName}`.trim());

  readonly initials = computed(() => {
    const fn = (this.profile.firstName || '').trim();
    const ln = (this.profile.lastName || '').trim();
    const first = fn ? fn[0] : '';
    const last = ln ? ln[0] : '';
    return (first + last).toUpperCase() || 'U';
  });

  readonly isDriver = computed(() => this.profile.role === 'DRIVER');

  // KT1 UI-only actions
  onEditProfile(): void {
    console.log('Edit Profile clicked');
  }
  onEditVehicle(): void {
    console.log('Edit Vehicle clicked');
  }
  onChangePassword(): void {
    console.log('Change Password clicked');
  }
  onUploadAvatar(): void {
    console.log('Upload Avatar clicked');
  }
}
