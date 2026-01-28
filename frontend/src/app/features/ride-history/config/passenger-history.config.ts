import { TableColumnConfig } from '../components/ride-history-table/ride-history-table.component';
import { RideDetailsConfig } from '../components/ride-details/ride-details.component';

/**
 * Configuration for passenger ride history view.
 * Passengers see their completed rides as passengers.
 */
export const passengerHistoryConfig = {
  tableColumns: [
    { key: 'dateTime', label: 'Date & Time', visible: true },
    { key: 'driver', label: 'Driver', visible: true },
    { key: 'route', label: 'Route', visible: true },
    { key: 'status', label: 'Status', visible: true },
  ] as TableColumnConfig[],

  detailsConfig: {
    showPassengers: false, // Don't show passenger list to other passengers
    showDriverInfo: true, // Show driver info to passengers
    showPanicAlert: true,
    showCancellationInfo: true,
    showEarnings: true, // Allow passengers to see cost
    showRatings: true,
    showRatingAction: true, // Allow passengers to rate rides
  } as RideDetailsConfig,

  pageTitle: 'My Rides',
  pageSubtitle: 'View all your completed and cancelled rides',
  showEarningsInTable: true,
};
