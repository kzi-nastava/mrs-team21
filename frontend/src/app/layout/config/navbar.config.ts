export interface NavItem {
  label: string;
  icon: string;
  path: string;
  section?: 'main' | 'account' | 'management' | 'monitoring';
  /** When true, item is shown but not clickable (route exists as placeholder). See docs/navbar-removed-features-spec.md */
  disabled?: boolean;
}

export interface NavigationConfig {
  passenger: NavItem[];
  driver: NavItem[];
  admin: NavItem[];
}

export const NAVIGATION_CONFIG: NavigationConfig = {
  passenger: [
    // Main Section
    { label: 'Order a Ride', icon: 'clock', path: '/order-ride', section: 'main' },
    { label: 'Ride Tracking', icon: 'map-pin', path: '/ride-tracking', section: 'main' },
    { label: 'Ride History', icon: 'file-text', path: '/ride-history', section: 'main' },
    { label: 'Favorite Routes', icon: 'bookmark', path: '/favorite-routes', section: 'main', disabled: true },

    // Account Section
    { label: 'Profile', icon: 'user', path: '/profile', section: 'account' },
    { label: 'Support', icon: 'message-circle', path: '/support', section: 'account', disabled: true },
  ],

  driver: [
    // Main Section
    { label: 'Ride Tracking', icon: 'map-pin', path: '/ride-tracking', section: 'main' },
    { label: 'Ride History', icon: 'file-text', path: '/driver/ride-history', section: 'main' },

    // Account Section
    { label: 'Profile', icon: 'user', path: '/profile', section: 'account' },
    { label: 'Support', icon: 'message-circle', path: '/support', section: 'account', disabled: true },
  ],

  admin: [
    // Main Section
    { label: 'Dashboard', icon: 'home', path: '/admin', section: 'main', disabled: true },

    // Monitoring Section
    { label: 'Active Rides', icon: 'map-pin', path: '/admin/active-rides', section: 'monitoring', disabled: true },
    { label: 'Ride History', icon: 'file-text', path: '/admin/ride-history', section: 'monitoring' },
    { label: 'Panic Notifications', icon: 'alert-triangle', path: '/admin/panic', section: 'monitoring', disabled: true },
    { label: 'Live Support / Chat', icon: 'message-circle', path: '/admin/support', section: 'monitoring', disabled: true },

    // Management Section
    { label: 'Register Driver', icon: 'truck', path: '/register-driver', section: 'management' },
    { label: 'Drivers', icon: 'truck', path: '/admin/drivers', section: 'management', disabled: true },
    { label: 'Passengers', icon: 'users', path: '/admin/passengers', section: 'management', disabled: true },
    { label: 'Reports', icon: 'bar-chart', path: '/admin/reports', section: 'management', disabled: true },
    { label: 'All Notifications', icon: 'bell', path: '/admin/notifications', section: 'management', disabled: true },

    // Account Section
    { label: 'Profile', icon: 'user', path: '/profile', section: 'account' },
  ],
};

// Section labels for display
export const SECTION_LABELS: Record<string, string> = {
  main: 'Menu',
  monitoring: 'Monitoring',
  management: 'Management',
  account: 'Account',
};
