import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export type AllowedRole = 'DRIVER' | 'PASSENGER' | 'ADMIN';

/**
 * Guard that requires the user to have one of the allowed roles.
 * Route should define data: { roles: ['ADMIN'] } (or multiple roles).
 * Redirects to / if the user's role is not in the allowed list.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const allowedRoles = (route.data['roles'] as AllowedRole[] | undefined) ?? [];
  const role = authService.getRole();

  if (role && allowedRoles.includes(role)) {
    return true;
  }

  return router.createUrlTree(['/']);
};
