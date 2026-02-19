export interface ReportDayDto {
  date: string;
  rideCount: number;
  distanceKm: number;
  cost: number;
}

export interface ReportChartResponse {
  from: string;
  to: string;
  dailyData: ReportDayDto[];
  totalRides: number;
  totalDistanceKm: number;
  totalCost: number;
  avgRidesPerDay: number;
  avgDistanceKmPerDay: number;
  avgCostPerDay: number;
}
