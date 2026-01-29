import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError, EMPTY } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toastService = inject(ToastService);
  const token = sessionStorage.getItem('token');
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }
  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401 || error.status === 403) {
        sessionStorage.removeItem('token');
        toastService.error('Your session expired. Please log in again.');
        router.navigate(['/login']);
        // Return EMPTY to complete the observable without propagating the error
        // since we've already handled it by redirecting to login
        return EMPTY;
      }
      return throwError(() => error);
    }),
  );
};
