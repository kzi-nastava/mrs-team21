import { Injectable } from '@angular/core';

export interface JwtPayload {
  sub: string; // email
  userId: number;
  role: 'DRIVER' | 'PASSENGER' | 'ADMIN';
  iat: number;
  exp: number;
}

/**
 * Centralized authentication service for JWT token management.
 * Provides methods to extract user information from the stored JWT token.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'token';

  /**
   * Get the current user's ID from the JWT token.
   */
  getUserId(): number | null {
    const payload = this.getTokenPayload();
    return payload?.userId ?? null;
  }

  /**
   * Get the current user's email from the JWT token.
   */
  getEmail(): string | null {
    const payload = this.getTokenPayload();
    return payload?.sub ?? null;
  }

  /**
   * Get the current user's role from the JWT token.
   */
  getRole(): 'DRIVER' | 'PASSENGER' | 'ADMIN' | null {
    const payload = this.getTokenPayload();
    return payload?.role ?? null;
  }

  /**
   * Check if the user is authenticated (has a valid token).
   */
  isAuthenticated(): boolean {
    const payload = this.getTokenPayload();
    if (!payload) return false;
    
    // Check if token is expired
    const now = Math.floor(Date.now() / 1000);
    return payload.exp > now;
  }

  /**
   * Check if the current user is a driver.
   */
  isDriver(): boolean {
    return this.getRole() === 'DRIVER';
  }

  /**
   * Check if the current user is a passenger.
   */
  isPassenger(): boolean {
    return this.getRole() === 'PASSENGER';
  }

  /**
   * Check if the current user is an admin.
   */
  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  /**
   * Log out the current user by removing the token.
   */
  logout(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }

  /**
   * Get the raw JWT token.
   */
  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Decode and return the JWT payload, or null if invalid/missing.
   */
  private getTokenPayload(): JwtPayload | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      return this.decodeJwt(token);
    } catch {
      return null;
    }
  }

  /**
   * Decode JWT token payload (middle segment).
   */
  private decodeJwt(token: string): JwtPayload {
    const payload = token.split('.')[1];
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decoded);
  }
}
