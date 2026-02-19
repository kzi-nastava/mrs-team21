import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { authGuard } from './shared/guards/auth.guard';
import { defaultRouteGuard } from './shared/guards/default-route.guard';
import { roleGuard } from './shared/guards/role.guard';

export const routes: Routes = [
  // Landing page (root - no layout). Authenticated users are redirected to their default page.
  {
    path: '',
    canActivate: [defaultRouteGuard],
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
  {
    path: 'activate/:token',
    loadComponent: () =>
      import('./features/auth/activate-account/activate-account.component').then(
        (m) => m.ActivateAccountComponent,
      ),
  },
  {
    path: 'activate-driver/:token',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
    data: { flow: 'driver-activation' },
  },

  // Authenticated routes under layout
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      // Admin only
      {
        path: 'register-driver',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/driver-registration/driver-registration.component').then(
            (m) => m.DriverRegistrationComponent,
          ),
      },
      // All roles
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile-page/profile-page.component').then(
            (m) => m.ProfilePageComponent,
          ),
      },
      {
        path: 'ride-tracking',
        loadComponent: () =>
          import('./features/ride-tracking/ride-tracking-landing.component').then(
            (m) => m.RideTrackingLandingComponent,
          ),
      },
      {
        path: 'ride-tracking/:rideId',
        loadComponent: () =>
          import('./features/ride-tracking/ride-tracking.component').then(
            (m) => m.RideTrackingComponent,
          ),
      },
      // Passenger only
      {
        path: 'ride-history',
        canActivate: [roleGuard],
        data: { roles: ['PASSENGER'] },
        loadComponent: () =>
          import(
            './features/ride-history/pages/passenger-history-page/passenger-history-page.component'
          ).then((m) => m.PassengerHistoryPageComponent),
      },
      {
        path: 'order-ride',
        canActivate: [roleGuard],
        data: { roles: ['PASSENGER'] },
        loadComponent: () =>
          import('./features/order-ride/order-ride.component').then((m) => m.OrderRideComponent),
      },
      // Passenger: placeholder routes (spec required, not yet implemented)
      {
        path: 'favorite-routes',
        canActivate: [roleGuard],
        data: { roles: ['PASSENGER'], featureName: 'Favorite Routes', specRef: '2.4.3' },
        loadComponent: () =>
          import('./shared/components/placeholder-feature/placeholder-feature.component').then(
            (m) => m.PlaceholderFeatureComponent,
          ),
      },
      {
        path: 'support',
        canActivate: [roleGuard],
        data: { roles: ['PASSENGER', 'DRIVER'] },
        loadComponent: () =>
          import('./features/support/support-page/support-page.component').then(
            (m) => m.SupportPageComponent,
          ),
      },
      // Driver only
      {
        path: 'driver',
        canActivate: [roleGuard],
        data: { roles: ['DRIVER'] },
        children: [
          {
            path: 'ride-history',
            loadComponent: () =>
              import(
                './features/ride-history/pages/driver-history-page/driver-history-page.component'
              ).then((m) => m.DriverHistoryPageComponent),
          },
        ],
      },
      // Admin only
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'Dashboard', specRef: '2.13' },
          },
          {
            path: 'ride-history',
            loadComponent: () =>
              import('./features/ride-history/pages/admin-history-page/admin-history-page.component')
                .then((m) => m.AdminHistoryPageComponent),
          },
          {
            path: 'active-rides',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'Active Rides', specRef: '2.13' },
          },
          {
            path: 'panic',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'Panic Notifications', specRef: '2.6.3' },
          },
          {
            path: 'support',
            loadComponent: () =>
              import('./features/support/admin-support-page/admin-support-page.component').then(
                (m) => m.AdminSupportPageComponent,
              ),
          },
          {
            path: 'drivers',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'Drivers', specRef: '2.12' },
          },
          {
            path: 'passengers',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'Passengers', specRef: '2.12' },
          },
          {
            path: 'reports',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'Reports', specRef: '2.10' },
          },
          {
            path: 'notifications',
            loadComponent: () =>
              import('./shared/components/placeholder-feature/placeholder-feature.component').then(
                (m) => m.PlaceholderFeatureComponent,
              ),
            data: { featureName: 'All Notifications' },
          },
        ],
      },
    ],
  },
  // Catch-all
  {
    path: '**',
    redirectTo: '',
  },
];
