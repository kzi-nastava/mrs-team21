import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RideApiService } from './services/ride-api.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-ride-tracking-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ride-tracking-landing.component.html',
  styleUrl: './ride-tracking-landing.component.scss',
})
export class RideTrackingLandingComponent implements OnInit {
  private readonly rideApi = inject(RideApiService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  loading = signal(true);
  noActiveRide = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.rideApi.getMyActiveRide().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res) {
          this.router.navigate(['/ride-tracking', res.rideId], { replaceUrl: true });
        } else {
          this.noActiveRide.set(true);
        }
      },
      error: (error) => {
        console.error('Failed to resolve active ride for tracking:', error);
        this.loading.set(false);
        this.errorMessage.set('Unable to load active ride right now. Please try again.');
      },
    });
  }

  isPassenger(): boolean {
    return this.auth.isPassenger();
  }

  isDriver(): boolean {
    return this.auth.isDriver();
  }
}
