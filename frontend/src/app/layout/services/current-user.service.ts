import { Injectable, signal } from '@angular/core';
import { UserProfile } from '../components/navbar/navbar.component';
import { ProfileData } from '../../features/profile/models/profile.model';

const DEFAULT_USER: UserProfile = {
  name: 'User',
  initials: 'US',
  role: 'Passenger',
  type: 'passenger',
  avatarUrl: null,
};

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly userState = signal<UserProfile>({ ...DEFAULT_USER });

  readonly user = this.userState.asReadonly();

  /**
   * Set role/type from token so navbar shows correct items before profile loads.
   * Called by layout when we have userId and role but before profile is loaded.
   */
  setRoleFromToken(role: 'DRIVER' | 'PASSENGER' | 'ADMIN', email?: string): void {
    const roleData = this.mapRole(role);
    this.userState.update((prev) => ({
      ...prev,
      role: roleData.label,
      type: roleData.type,
      ...(email && { name: email, initials: this.getInitials(email) }),
    }));
  }

  /**
   * Set full user from profile (after profile API load).
   */
  setFromProfile(profile: ProfileData): void {
    const fullName = `${profile.firstName} ${profile.lastName}`.trim();
    const roleData = this.mapRole(profile.role);
    this.userState.set({
      name: fullName || 'User',
      initials: this.getInitials(fullName || 'User'),
      role: roleData.label,
      type: roleData.type,
      avatarUrl: profile.avatarUrl ?? null,
    });
  }

  /**
   * Update only the avatar URL (e.g. after profile picture update).
   */
  updateAvatar(url: string | null): void {
    this.userState.update((prev) => ({ ...prev, avatarUrl: url }));
  }

  /**
   * Reset to default (e.g. on logout). Optional; layout may unmount on logout.
   */
  reset(): void {
    this.userState.set({ ...DEFAULT_USER });
  }

  private mapRole(role: string): { label: string; type: UserProfile['type'] } {
    const r = (role ?? '').toUpperCase();
    if (r === 'DRIVER') return { label: 'Driver', type: 'driver' };
    if (r === 'ADMIN') return { label: 'Admin', type: 'admin' };
    return { label: 'Passenger', type: 'passenger' };
  }

  private getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2) || 'US';
  }
}
