import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';

export const routes: Routes = [
  // Landing page (root - no layout)
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing-page/landing-page.component').then(
        (m) => m.LandingPageComponent,
      ),
  },

  // Auth routes (no layout)
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register-customer',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'reset-password/:token',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
  },

  // Non-auth routes under layout
  {
    path: '',
    component: LayoutComponent,
    children: [
      // Admin
      {
        path: 'register-driver',
        loadComponent: () =>
          import('./features/admin/driver-registration/driver-registration.component').then(
            (m) => m.DriverRegistrationComponent,
          ),
      },
      // User
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile-page/profile-page.component').then(
            (m) => m.ProfilePageComponent,
          ),
      },
      {
        path: 'driver-history',
        loadComponent: () =>
          import('./features/driver-history/driver-history.component').then(
            (m) => m.DriverHistoryComponent,
          ),
      },
      // Ride
      {
        path: 'ride-tracking/:rideId',
        loadComponent: () =>
          import('./features/ride-tracking/ride-tracking.component').then(
            (m) => m.RideTrackingComponent,
          ),
      },
      {
        path: 'order-ride',
        loadComponent: () =>
          import('./features/order-ride/order-ride.component').then((m) => m.OrderRideComponent),
      },
    ],
  },
  // Catch-all
  {
    path: '**',
    redirectTo: '',
  },
];
