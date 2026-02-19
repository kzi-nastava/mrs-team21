import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard for the root path (''). When the user is authenticated, redirects them
 * to their role-specific default page instead of showing the landing page.
 * - PASSENGER -> /order-ride
 * - DRIVER -> /driver/ride-history
 * - ADMIN -> /admin/ride-history
 */
export const defaultRouteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return true;
  }

  const defaultRoute = auth.getDefaultRoute();
  if (defaultRoute === '/') {
    return true;
  }
  return router.createUrlTree([defaultRoute]);
};
