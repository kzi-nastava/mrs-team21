package com.drumigo.mobile.ui.reports;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.ReportApiService;
import com.drumigo.mobile.data.model.report.ReportChartResponse;
import com.drumigo.mobile.data.model.report.ReportDayDto;
import com.drumigo.mobile.databinding.FragmentReportsBinding;
import com.drumigo.mobile.session.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reports screen: ride statistics for the current user (passenger or driver).
 * Date range filter, summary (totals and averages), and bar charts like frontend.
 */
public class ReportsFragment extends Fragment {

    /** Frontend chart colors: primary, primary_light, accent */
    private static final int[] CHART_COLORS = { 0xFF5B4CDB, 0xFF7B6EE8, 0xFFFF7F5C };

    private static final String ROLE_DRIVER = "DRIVER";

    private FragmentReportsBinding binding;
    private ReportApiService reportApiService;
    private SessionManager sessionManager;
    private SimpleDateFormat dateFormat;
    private Calendar fromDate;
    private Calendar toDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reportApiService = ApiClient.getReportApiService();
        sessionManager = SessionManager.getInstance(requireContext());
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        toDate = Calendar.getInstance();
        fromDate = Calendar.getInstance();
        fromDate.add(Calendar.DAY_OF_MONTH, -30);

        binding.reportsFromDateText.setText(dateFormat.format(fromDate.getTime()));
        binding.reportsToDateText.setText(dateFormat.format(toDate.getTime()));

        binding.reportsFromDateText.setOnClickListener(v -> showDatePicker(true));
        binding.reportsToDateText.setOnClickListener(v -> showDatePicker(false));

        binding.reportsGenerateButton.setOnClickListener(v -> loadReport());

        loadReport();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void showDatePicker(boolean isFrom) {
        Calendar target = isFrom ? fromDate : toDate;
        DatePickerDialog dialog = new DatePickerDialog(
            requireContext(),
            0,
            (view, year, month, dayOfMonth) -> {
                target.set(year, month, dayOfMonth);
                if (isFrom) {
                    binding.reportsFromDateText.setText(dateFormat.format(target.getTime()));
                } else {
                    binding.reportsToDateText.setText(dateFormat.format(target.getTime()));
                }
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void loadReport() {
        String fromIso = toIsoStartOfDay(fromDate);
        String toIso = toIsoEndOfDay(toDate);
        if (fromIso == null || toIso == null) {
            binding.reportsErrorText.setVisibility(View.VISIBLE);
            binding.reportsErrorText.setText(R.string.reports_select_dates);
            return;
        }

        binding.reportsErrorText.setVisibility(View.GONE);
        binding.reportsSummarySection.setVisibility(View.GONE);
        setLoading(true);

        reportApiService.getMyReport(fromIso, toIso).enqueue(new Callback<ReportChartResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<ReportChartResponse> call,
                @NonNull Response<ReportChartResponse> response
            ) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    binding.reportsErrorText.setText(getString(R.string.reports_error));
                    binding.reportsErrorText.setVisibility(View.VISIBLE);
                    return;
                }
                ReportChartResponse r = response.body();
                bindReport(r);
            }

            @Override
            public void onFailure(@NonNull Call<ReportChartResponse> call, @NonNull Throwable t) {
                setLoading(false);
                binding.reportsErrorText.setText(getString(R.string.reports_error));
                binding.reportsErrorText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindReport(ReportChartResponse r) {
        binding.reportsErrorText.setVisibility(View.GONE);
        binding.reportsSummarySection.setVisibility(View.VISIBLE);

        String role = sessionManager.getRole();
        boolean isDriver = role != null && ROLE_DRIVER.equals(role.trim().toUpperCase(Locale.ENGLISH));
        binding.reportsSummaryMoneyLabel.setText(
            isDriver ? R.string.reports_money_earned : R.string.reports_money_spent);

        binding.reportsSummaryRidesCumulative.setText(String.valueOf(r.totalRides));
        binding.reportsSummaryRidesAvg.setText(String.format(Locale.US, "%.1f", r.avgRidesPerDay));

        binding.reportsSummaryDistanceCumulative.setText(String.valueOf(r.totalDistanceKm));
        binding.reportsSummaryDistanceAvg.setText(String.format(Locale.US, "%.2f", r.avgDistanceKmPerDay));

        binding.reportsSummaryMoneyCumulative.setText(String.format(Locale.US, "%.2f", r.totalCost));
        binding.reportsSummaryMoneyAvg.setText(String.format(Locale.US, "%.2f", r.avgCostPerDay));

        binding.reportsChartMoneyTitle.setText(
            getString(isDriver ? R.string.reports_money_earned : R.string.reports_money_spent) + " per day");

        List<ReportDayDto> dailyData = r.dailyData;
        if (dailyData != null && !dailyData.isEmpty()) {
            List<String> labels = formatChartDates(dailyData);
            setupBarChart(binding.reportsChartRides, labels, dailyData, true, false, false, CHART_COLORS[0]);
            setupBarChart(binding.reportsChartDistance, labels, dailyData, false, true, false, CHART_COLORS[1]);
            setupBarChart(binding.reportsChartCost, labels, dailyData, false, false, true, CHART_COLORS[2]);
        } else {
            clearCharts();
        }
    }

    /** Short date for chart x-axis (e.g. "20 Jan") like frontend. */
    private static List<String> formatChartDates(List<ReportDayDto> dailyData) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<String> out = new ArrayList<>(dailyData.size());
        for (ReportDayDto d : dailyData) {
            String date = d.date;
            if (date == null || date.length() < 10) {
                out.add(date != null ? date : "");
                continue;
            }
            try {
                int year = Integer.parseInt(date.substring(0, 4));
                int month = Integer.parseInt(date.substring(5, 7));
                int day = Integer.parseInt(date.substring(8, 10));
                String mon = (month >= 1 && month <= 12) ? months[month - 1] : String.valueOf(month);
                out.add(day + " " + mon);
            } catch (Exception e) {
                out.add(date);
            }
        }
        return out;
    }

    private void setupBarChart(BarChart chart, List<String> labels,
                               List<ReportDayDto> dailyData,
                               boolean useRides, boolean useDistance, boolean useCost,
                               int barColor) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dailyData.size(); i++) {
            ReportDayDto d = dailyData.get(i);
            float val = useRides ? d.rideCount : (useDistance ? (float) d.distanceKm : (float) d.cost);
            entries.add(new BarEntry(i, val));
        }
        BarDataSet set = new BarDataSet(entries, null);
        set.setColor(barColor);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.parseColor("#4a4a4a"));

        BarData data = new BarData(set);
        data.setBarWidth(0.45f);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setGranularityEnabled(true);
        chart.getXAxis().setTextColor(Color.parseColor("#4a4a4a"));
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(Color.parseColor("#4a4a4a"));
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setData(data);
        chart.setFitBars(true);
        chart.animateY(400);
        chart.invalidate();
    }

    private void clearCharts() {
        binding.reportsChartRides.clear();
        binding.reportsChartRides.invalidate();
        binding.reportsChartDistance.clear();
        binding.reportsChartDistance.invalidate();
        binding.reportsChartCost.clear();
        binding.reportsChartCost.invalidate();
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.reportsLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.reportsGenerateButton.setEnabled(!loading);
    }

    private static String toIsoStartOfDay(Calendar calendar) {
        if (calendar == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate localDate = Instant.ofEpochMilli(calendar.getTimeInMillis())
            .atZone(zone)
            .toLocalDate();
        ZonedDateTime start = localDate.atStartOfDay(zone);
        return start.toInstant().toString();
    }

    private static String toIsoEndOfDay(Calendar calendar) {
        if (calendar == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate localDate = Instant.ofEpochMilli(calendar.getTimeInMillis())
            .atZone(zone)
            .toLocalDate();
        ZonedDateTime end = localDate.atTime(LocalTime.MAX).atZone(zone);
        return end.toInstant().toString();
    }
}
