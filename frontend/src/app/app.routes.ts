import { Routes } from '@angular/router';

export const routes: Routes = [
  // Auth routes
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

  // Admin routes
  {
    path: 'register-driver',
    loadComponent: () =>
      import('./features/admin/driver-registration/driver-registration.component').then(
        (m) => m.DriverRegistrationComponent,
      ),
  },

  // User routes
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

  // Ride routes
  {
    path: 'ride-tracking/:rideId',
    loadComponent: () =>
      import('./features/ride-tracking/ride-tracking.component').then(
        (m) => m.RideTrackingComponent,
      ),
  },

  // Landing page
  {
    path: 'landing',
    loadComponent: () =>
      import('./features/landing/landing-page/landing-page.component').then(
        (m) => m.LandingPageComponent,
      ),
  },

  // Catch-all routes
  {
    path: '',
    redirectTo: 'landing',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'landing',
  },
];
