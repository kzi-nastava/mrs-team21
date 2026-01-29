export interface RideFilter {
  startDate: string;
  endDate: string;
  status?: 'all' | 'completed' | 'cancelled';
  vehicleType?: string;
}
