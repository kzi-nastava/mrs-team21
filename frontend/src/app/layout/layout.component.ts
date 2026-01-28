import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent, UserProfile } from './components/navbar/navbar.component';
import { AuthService } from '../shared/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
})
export class LayoutComponent implements OnInit {
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
    const email = this.authService.getEmail();
    const role = this.authService.getRole();

    if (email && role) {
      this.user = {
        name: email,
        initials: this.getInitials(email),
        role: role === 'PASSENGER' ? 'Passenger' : 'Driver',
        type: role === 'PASSENGER' ? 'passenger' : 'driver',
      };
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
