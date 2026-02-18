import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
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
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  user: UserProfile = {
    name: 'User',
    initials: 'US',
    role: 'Passenger',
    type: 'passenger',
  };

  ngOnInit(): void {
    this.loadUserFromToken();
    this.driverLocationPingService.start();
    this.authService.loginSuccess$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.loadUserFromToken();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
      this.profileService.getProfile().subscribe({
        next: (profile) => {
          const fullName = `${profile.firstName} ${profile.lastName}`;
          this.user = {
            ...this.user,
            name: fullName,
            initials: this.getInitials(fullName),
            avatarUrl: profile.avatarUrl,
          };
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to load profile:', err);
          if (email) {
            this.user = {
              ...this.user,
              name: email,
              initials: this.getInitials(email),
            };
            this.cdr.detectChanges();
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
