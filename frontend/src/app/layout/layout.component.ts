import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent, UserProfile } from './components/navbar/navbar.component';
import { ProfileApiService } from '../features/profile/services/profile-api.service';
import { AuthService } from '../shared/services/auth.service';
import { DriverLocationPingService } from '../shared/services/driver-location-ping.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
})
export class LayoutComponent implements OnInit, OnDestroy {

  private profileService = inject(ProfileApiService);
  private readonly authService = inject(AuthService);
  private readonly driverLocationPingService = inject(DriverLocationPingService);

  user: UserProfile = {
    name: 'User',
    initials: 'US',
    role: 'Passenger',
    type: 'passenger',
  };

  ngOnInit(): void {
    this.loadUserFromToken();
    this.driverLocationPingService.start();
  }

  ngOnDestroy(): void {
    this.driverLocationPingService.stop();
  }

  private loadUserFromToken(): void {
    const userId = this.authService.getUserId();
    const email = this.authService.getEmail();
    const role = this.authService.getRole();

    if (userId && role) {
      const roleData = this.mapRole(role);
      // Set role/type immediately from token so navbar shows correct items (driver vs passenger) before profile loads
      this.user = {
        ...this.user,
        role: roleData.label,
        type: roleData.type,
      };
      this.profileService.getProfile(userId).subscribe({
        next: (profile) => {
          const fullName = `${profile.firstName} ${profile.lastName}`;
          this.user = {
            ...this.user,
            name: fullName,
            initials: this.getInitials(fullName),
            avatarUrl: profile.avatarUrl,
          };
        },
        error: (err) => {
          console.error('Failed to load profile:', err);
          if (email) {
            this.user = {
              ...this.user,
              name: email,
              initials: this.getInitials(email),
            };
          }
        },
      });
      return;
    }

    if (email && role && this.user.name === 'User') {
      const roleData = this.mapRole(role);
      this.user = {
        name: email,
        initials: this.getInitials(email),
        role: roleData.label,
        type: roleData.type,
      };
    }
  }

  private mapRole(role: 'DRIVER' | 'PASSENGER' | 'ADMIN'): { label: string; type: UserProfile['type'] } {
    const r = (role ?? '').toUpperCase();
    if (r === 'DRIVER') {
      return { label: 'Driver', type: 'driver' };
    }
    if (r === 'ADMIN') {
      return { label: 'Admin', type: 'admin' };
    }
    return { label: 'Passenger', type: 'passenger' };
  }

  private getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}
