import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
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
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },

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
  {
    path: 'landing',
    loadComponent: () =>
      import('./features/landing/landing-page/landing-page.component').then(
        (m) => m.LandingPageComponent,
      ),
  },

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
