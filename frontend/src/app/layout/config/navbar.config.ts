export interface NavItem {
  label: string;
  icon: string;
  path: string;
  section?: 'main' | 'account' | 'management' | 'monitoring';
}

export interface NavigationConfig {
  passenger: NavItem[];
  driver: NavItem[];
  admin: NavItem[];
}

export const NAVIGATION_CONFIG: NavigationConfig = {
  passenger: [
    // Main Section
    { label: 'Book a Ride', icon: 'clock', path: '/landing', section: 'main' },
    { label: 'Ride Tracking', icon: 'map-pin', path: '/ride-tracking/1', section: 'main' },
    { label: 'Ride History', icon: 'file-text', path: '/driver-history', section: 'main' },
    { label: 'Favorite Routes', icon: 'bookmark', path: '#', section: 'main' },

    // Account Section
    { label: 'Profile', icon: 'user', path: '/profile', section: 'account' },
    { label: 'Settings', icon: 'settings', path: '#', section: 'account' },
    { label: 'Support', icon: 'message-circle', path: '#', section: 'account' },
  ],

  driver: [
    // Main Section
    { label: 'Ride Tracking', icon: 'map-pin', path: '/ride-tracking/1', section: 'main' },
    { label: 'Ride History', icon: 'file-text', path: '/driver-history', section: 'main' },

    // Account Section
    { label: 'Profile', icon: 'user', path: '/profile', section: 'account' },
    { label: 'Settings', icon: 'settings', path: '#', section: 'account' },
    { label: 'Support', icon: 'message-circle', path: '#', section: 'account' },
  ],

  admin: [
    // Main Section
    { label: 'Dashboard', icon: 'home', path: '#', section: 'main' },

    // Monitoring Section
    { label: 'Active Rides', icon: 'map-pin', path: '#', section: 'monitoring' },
    { label: 'Ride History', icon: 'file-text', path: '/driver-history', section: 'monitoring' },
    { label: 'Panic Notifications', icon: 'alert-triangle', path: '#', section: 'monitoring' },
    { label: 'Live Support / Chat', icon: 'message-circle', path: '#', section: 'monitoring' },

    // Management Section
    { label: 'Drivers', icon: 'truck', path: '#', section: 'management' },
    { label: 'Passengers', icon: 'users', path: '#', section: 'management' },
    { label: 'Reports', icon: 'bar-chart', path: '#', section: 'management' },
    { label: 'All Notifications', icon: 'bell', path: '#', section: 'management' },

    // Account Section
    { label: 'Profile', icon: 'user', path: '/profile', section: 'account' },
    { label: 'Settings', icon: 'settings', path: '#', section: 'account' },
  ],
};

// Section labels for display
export const SECTION_LABELS: Record<string, string> = {
  main: 'Menu',
  monitoring: 'Monitoring',
  management: 'Management',
  account: 'Account',
};
