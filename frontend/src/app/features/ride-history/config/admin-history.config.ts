import { TableColumnConfig } from '../components/ride-history-table/ride-history-table.component';
import { RideDetailsConfig } from '../components/ride-details/ride-details.component';

/**
 * Configuration for admin ride history view.
 * Admins see all system rides with full information visibility.
 */
export const adminHistoryConfig = {
  tableColumns: [
    { key: 'dateTime', label: 'Date & Time', visible: true },
    { key: 'driver', label: 'Driver', visible: true },
    { key: 'passengers', label: 'Passengers', visible: true },
    { key: 'route', label: 'Route', visible: true },
    { key: 'status', label: 'Status', visible: true },
  ] as TableColumnConfig[],

  detailsConfig: {
    showPassengers: true,
    showDriverInfo: true, // Admins see driver info
    showPanicAlert: true,
    showCancellationInfo: true,
    showEarnings: true,
    showRatings: true,
    showRatingAction: false, // Admins don't rate rides
  } as RideDetailsConfig,

  pageTitle: 'System Ride History',
  pageSubtitle: 'View and monitor all rides in the system',
  showEarningsInTable: true,
};
