import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent, UserProfile } from './components/navbar/navbar.component';
import { ProfileApiService } from '../features/profile/services/profile-api.service';
import { AuthService } from '../shared/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
})
export class LayoutComponent implements OnInit {

  private profileService = inject(ProfileApiService);
  private readonly authService = inject(AuthService);

  user: UserProfile = {
    name: 'User',
    initials: 'US',
    role: 'Passenger',
    type: 'passenger',
  };

  ngOnInit(): void {
    this.loadUserFromToken();
  }

  private loadUserFromToken(): void {

    const token = sessionStorage.getItem('token');
    if (token) {
      try {
        const payload = this.decodeJwtPayload(token);
        const userIdRaw = payload.userId || payload.sub;
        const userId = typeof userIdRaw === 'string' ? parseInt(userIdRaw, 10) : userIdRaw;
        const role = payload.role;

        // Fetch full profile including avatar
        if (userId && !isNaN(userId)) {
          this.profileService.getProfile(userId).subscribe({
            next: (profile) => {
              this.user = {
                name: `${profile.firstName} ${profile.lastName}`,
                initials: this.getInitials(`${profile.firstName} ${profile.lastName}`),
                role: role === 'PASSENGER' ? 'Passenger' : role === 'DRIVER' ? 'Driver' : 'Admin',
                type: role === 'PASSENGER' ? 'passenger' : role === 'DRIVER' ? 'driver' : 'admin',
                avatarUrl: profile.avatarUrl,
              };
            },
            error: (err) => {
              console.error('Failed to load profile:', err);
              // Fallback to basic info from token
              this.user = {
                name: payload.sub || 'User',
                initials: this.getInitials(payload.sub || 'User'),
                role: role === 'PASSENGER' ? 'Passenger' : role === 'DRIVER' ? 'Driver' : 'Admin',
                type: role === 'PASSENGER' ? 'passenger' : role === 'DRIVER' ? 'driver' : 'admin',
              };
            },
          });
        }
      } catch (error) {
        console.error('Error decoding token:', error);
      }
    }
    
    // Fallback: use email and role from AuthService if token parsing fails
    const email = this.authService.getEmail();
    const role = this.authService.getRole();
    if (email && role && this.user.name === 'User') {
      this.user = {
        name: email,
        initials: this.getInitials(email),
        role: role === 'PASSENGER' ? 'Passenger' : role === 'DRIVER' ? 'Driver' : 'Admin',
        type: role === 'PASSENGER' ? 'passenger' : role === 'DRIVER' ? 'driver' : 'admin',
      };
    }
  }

  private decodeJwtPayload(token: string): { userId?: number; sub?: string; role?: string } {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return {};
    }
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
