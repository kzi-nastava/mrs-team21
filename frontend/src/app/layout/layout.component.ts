import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { NavbarComponent } from './components/navbar/navbar.component';
import { ProfileApiService } from '../features/profile/services/profile-api.service';
import { AuthService } from '../shared/services/auth.service';
import { DriverLocationPingService } from '../shared/services/driver-location-ping.service';
import { CurrentUserService } from './services/current-user.service';

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
  private readonly currentUserService = inject(CurrentUserService);
  private readonly destroy$ = new Subject<void>();

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
      this.currentUserService.setRoleFromToken(role, email ?? undefined);
      this.profileService.getProfile().subscribe({
        next: (profile) => {
          this.currentUserService.setFromProfile(profile);
        },
        error: (err) => {
          console.error('Failed to load profile:', err);
          this.currentUserService.updateAvatar(null);
        },
      });
      return;
    }

    if (email && role) {
      this.currentUserService.setRoleFromToken(role, email);
    }
  }
}
