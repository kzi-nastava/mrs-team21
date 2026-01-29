import { TableColumnConfig } from '../components/ride-history-table/ride-history-table.component';
import { RideDetailsConfig } from '../components/ride-details/ride-details.component';

/**
 * Configuration for driver ride history view.
 * Drivers see all their completed rides with earnings and passenger info.
 */
export const driverHistoryConfig = {
  tableColumns: [
    { key: 'dateTime', label: 'Date & Time', visible: true },
    { key: 'passengers', label: 'Passengers', visible: true },
    { key: 'route', label: 'Route', visible: true },
    { key: 'status', label: 'Status', visible: true },
  ] as TableColumnConfig[],

  detailsConfig: {
    showPassengers: true,
    showDriverInfo: false, // Driver is viewing their own rides
    showPanicAlert: true,
    showCancellationInfo: true,
    showEarnings: true, // Drivers see earnings
    showRatings: true,
    showRatingAction: false, // Drivers don't rate - only passengers do
  } as RideDetailsConfig,

  pageTitle: 'Ride History',
  pageSubtitle: 'View all your completed and cancelled rides',
  showEarningsInTable: true,
};
