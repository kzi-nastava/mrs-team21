import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent, UserProfile } from './components/navbar/navbar.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
})
export class LayoutComponent implements OnInit {
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
        this.user = {
          name: payload.sub || 'User',
          initials: this.getInitials(payload.sub || 'User'),
          role: payload.role === 'PASSENGER' ? 'Passenger' : 'Driver',
          type: payload.role === 'PASSENGER' ? 'passenger' : 'driver',
        };
      } catch (error) {
        console.error('Error decoding token:', error);
      }
    }
  }

  private decodeJwtPayload(token: string): any {
    const payload = token.split('.')[1];
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decoded);
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
