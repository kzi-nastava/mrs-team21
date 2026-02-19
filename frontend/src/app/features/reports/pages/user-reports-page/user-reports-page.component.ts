import {
  Component,
  inject,
  signal,
  computed,
  DestroyRef,
  OnInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexChart, ApexAxisChartSeries, ApexXAxis, ApexYAxis, ApexPlotOptions } from 'ng-apexcharts';
import { ReportsApiService } from '../../services/reports-api.service';
import { CurrentUserService } from '../../../../layout/services/current-user.service';
import { ReportChartResponse } from '../../models';

function last30Days(): { from: string; to: string } {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - 30);
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  };
}

function toInstant(dateStr: string, endOfDay: boolean): string {
  if (!dateStr) return '';
  const d = new Date(dateStr + (endOfDay ? 'T23:59:59.999Z' : 'T00:00:00.000Z'));
  return d.toISOString();
}

/** Short date for chart x-axis so bars and labels align (e.g. "20 Jan"). */
function formatChartDate(isoDate: string): string {
  if (!isoDate) return '';
  const d = new Date(isoDate + 'T12:00:00Z');
  const day = d.getUTCDate();
  const mon = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][d.getUTCMonth()];
  return `${day} ${mon}`;
}

const CHART_COLORS = ['#5b4cdb', '#7b6ee8', '#ff7f5c'];
const CHART_BASE: ApexChart = {
  type: 'bar',
  width: '100%',
  fontFamily: 'DM Sans, sans-serif',
  toolbar: { show: false },
  zoom: { enabled: false },
  animations: { enabled: true, speed: 400 },
  events: {
    mounted: (chartContext) => {
      chartContext?.windowResizeHandler?.();
    },
  },
};
const CHART_PLOT: ApexPlotOptions = {
  bar: {
    borderRadius: 8,
    columnWidth: '55%',
    distributed: false,
  },
};
const CHART_XAXIS = (categories: string[], shortLabels?: string[]): ApexXAxis => ({
  categories: shortLabels ?? categories,
  labels: {
    style: { colors: '#4a4a4a', fontSize: '11px' },
    rotate: 0,
  },
  axisBorder: { show: true, color: '#e8e4dd' },
  axisTicks: { show: false },
  tickPlacement: 'on',
});
@Component({
  selector: 'app-user-reports-page',
  standalone: true,
  imports: [CommonModule, FormsModule, NgApexchartsModule],
  templateUrl: './user-reports-page.component.html',
  styleUrl: './user-reports-page.component.scss',
})
export class UserReportsPageComponent implements OnInit {
  private readonly reportsApi = inject(ReportsApiService);
  private readonly currentUser = inject(CurrentUserService);
  private readonly destroyRef = inject(DestroyRef);

  fromDate = signal('');
  toDate = signal('');
  report = signal<ReportChartResponse | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  isDriver = computed(() => this.currentUser.user().type === 'driver');
  moneyLabel = computed(() => (this.isDriver() ? 'Money earned' : 'Money spent'));

  chartRides = signal<{ series: ApexAxisChartSeries; chart: ApexChart; xaxis: ApexXAxis; colors: string[]; plotOptions: ApexPlotOptions }>({
    series: [{ name: 'Rides', data: [] }],
    chart: CHART_BASE,
    xaxis: CHART_XAXIS([]),
    colors: [CHART_COLORS[0]],
    plotOptions: CHART_PLOT,
  });
  chartKm = signal<{ series: ApexAxisChartSeries; chart: ApexChart; xaxis: ApexXAxis; colors: string[]; plotOptions: ApexPlotOptions }>({
    series: [{ name: 'Distance (km)', data: [] }],
    chart: CHART_BASE,
    xaxis: CHART_XAXIS([]),
    colors: [CHART_COLORS[1]],
    plotOptions: CHART_PLOT,
  });
  chartCost = signal<{ series: ApexAxisChartSeries; chart: ApexChart; xaxis: ApexXAxis; colors: string[]; plotOptions: ApexPlotOptions }>({
    series: [{ name: 'Amount', data: [] }],
    chart: CHART_BASE,
    xaxis: CHART_XAXIS([]),
    colors: [CHART_COLORS[2]],
    plotOptions: CHART_PLOT,
  });

  readonly chartTooltipRides = { theme: 'light' as const, y: { formatter: (v: number) => String(Math.round(v)) } };
  readonly chartTooltipKm = { theme: 'light' as const, y: { formatter: (v: number) => v + ' km' } };
  readonly chartTooltipCost = { theme: 'light' as const, y: { formatter: (v: number) => Number(v).toFixed(2) } };
  readonly chartGrid = { borderColor: '#e8e4dd', strokeDashArray: 4, xaxis: { lines: { show: false } } };
  readonly chartYaxis = { labels: { style: { colors: '#4a4a4a' } }, axisBorder: { show: false }, axisTicks: { show: false } };
  /** Integer scale for rides: show 0, 1, 2, 3... on the left. */
  readonly chartYaxisRides: ApexYAxis = {
    min: 0,
    tickAmount: 1,
    forceNiceScale: true,
    labels: { style: { colors: '#4a4a4a' }, formatter: (v: number) => String(Math.round(v)) },
    axisBorder: { show: false },
    axisTicks: { show: false },
  };
  readonly chartDataLabels = { enabled: false };

  constructor() {
    const { from, to } = last30Days();
    this.fromDate.set(from);
    this.toDate.set(to);
  }

  ngOnInit(): void {
    this.loadReport();
  }

  loadReport(): void {
    const from = this.fromDate();
    const to = this.toDate();
    if (!from || !to) {
      this.error.set('Please select both From and To dates.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    const fromInstant = toInstant(from, false);
    const toInstantVal = toInstant(to, true);
    this.reportsApi
      .getMyReport(fromInstant, toInstantVal)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.report.set(res);
          const labels = res.dailyData.map((d) => d.date);
          const shortLabels = labels.map(formatChartDate);
          this.chartRides.set({
            ...this.chartRides(),
            series: [{ name: 'Rides', data: res.dailyData.map((d) => d.rideCount) }],
            xaxis: CHART_XAXIS(labels, shortLabels),
          });
          this.chartKm.set({
            ...this.chartKm(),
            series: [{ name: 'Distance (km)', data: res.dailyData.map((d) => d.distanceKm) }],
            xaxis: CHART_XAXIS(labels, shortLabels),
          });
          this.chartCost.set({
            ...this.chartCost(),
            series: [{ name: this.moneyLabel(), data: res.dailyData.map((d) => d.cost) }],
            xaxis: CHART_XAXIS(labels, shortLabels),
          });
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load report.');
          this.loading.set(false);
        },
      });
  }

  onApply(): void {
    this.loadReport();
  }
}
