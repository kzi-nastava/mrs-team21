package com.drumigo.mobile.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.DriverApiService;
import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.history.DriverRideHistoryItemResponse;
import com.drumigo.mobile.data.model.history.PageResponse;
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

public class RideHistoryActivity extends AppCompatActivity {

    private static final String TAG = "RideHistoryActivity";
    private static final long DRIVER_ID = 7001L;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SORT = "requestedAt,desc";

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private TextView fromDateText, toDateText, resultsCountText;
    private Button applyFilterButton;
    private Calendar fromDate, toDate;
    private SimpleDateFormat dateFormat;
    private DriverApiService driverApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_ride_history);

            dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            fromDate = Calendar.getInstance();
            fromDate.set(2024, 0, 1);
            toDate = Calendar.getInstance();

            setupToolbar();
            initializeViews();
            setupRecyclerView();
            setupDatePickers();
            driverApiService = ApiClient.getDriverApiService();
            loadRideHistory();
        } catch (Exception e) {
            // Log and show a toast instead of crashing so we can diagnose in logs
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Unable to open Ride History (see logs)", Toast.LENGTH_LONG).show();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        } else {
            Log.w(TAG, "Toolbar not found in layout");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRides);
        fromDateText = findViewById(R.id.fromDateText);
        toDateText = findViewById(R.id.toDateText);
        resultsCountText = findViewById(R.id.resultsCountText);
        applyFilterButton = findViewById(R.id.applyFilterButton);

        if (fromDateText != null) fromDateText.setText(dateFormat.format(fromDate.getTime()));
        if (toDateText != null) toDateText.setText(dateFormat.format(toDate.getTime()));

        if (applyFilterButton != null) {
            applyFilterButton.setOnClickListener(v -> applyFilter());
        } else {
            Log.w(TAG, "applyFilterButton not found in layout");
        }
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter(new ArrayList<>(), this);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        } else {
            Log.w(TAG, "RecyclerView not found in layout");
        }
    }

    private void setupDatePickers() {
        if (fromDateText != null) fromDateText.setOnClickListener(v -> showDatePicker(true));
        if (toDateText != null) toDateText.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = isFromDate ? fromDate : toDate;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                0,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isFromDate) {
                        if (fromDateText != null) fromDateText.setText(dateFormat.format(calendar.getTime()));
                    } else {
                        if (toDateText != null) toDateText.setText(dateFormat.format(calendar.getTime()));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void applyFilter() {
        loadRideHistory();
    }

    private void loadRideHistory() {
        if (driverApiService == null) {
            driverApiService = ApiClient.getDriverApiService();
        }
        if (driverApiService == null) {
            showHistoryLoadError();
            return;
        }

        String from = toIsoStartOfDay(fromDate);
        String to = toIsoEndOfDay(toDate);
        driverApiService.getDriverRideHistory(
            DRIVER_ID,
            from,
            to,
            DEFAULT_PAGE,
            DEFAULT_PAGE_SIZE,
            DEFAULT_SORT
        ).enqueue(new Callback<PageResponse<DriverRideHistoryItemResponse>>() {
            @Override
            public void onResponse(
                Call<PageResponse<DriverRideHistoryItemResponse>> call,
                Response<PageResponse<DriverRideHistoryItemResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                    showHistoryLoadError();
                    updateHistoryResults(new ArrayList<>());
                    return;
                }
                List<Ride> rides = new ArrayList<>();
                for (DriverRideHistoryItemResponse item : response.body().content) {
                    Ride mapped = DriverRideHistoryMapper.toRide(item);
                    if (mapped != null) {
                        rides.add(mapped);
                    }
                }
                updateHistoryResults(rides);
            }

            @Override
            public void onFailure(
                Call<PageResponse<DriverRideHistoryItemResponse>> call,
                Throwable t
            ) {
                showHistoryLoadError();
                updateHistoryResults(new ArrayList<>());
            }
        });
    }

    private void updateHistoryResults(List<Ride> rides) {
        if (adapter != null) {
            adapter.updateRides(rides);
        }
        if (resultsCountText != null) {
            resultsCountText.setText("Showing " + (rides == null ? 0 : rides.size()) + " rides");
        }
    }

    private void showHistoryLoadError() {
        Toast.makeText(this, "Failed to load ride history", Toast.LENGTH_SHORT).show();
    }

    private String toIsoStartOfDay(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate localDate = Instant.ofEpochMilli(calendar.getTimeInMillis())
            .atZone(zone)
            .toLocalDate();
        ZonedDateTime start = localDate.atStartOfDay(zone);
        return start.toInstant().toString();
    }

    private String toIsoEndOfDay(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate localDate = Instant.ofEpochMilli(calendar.getTimeInMillis())
            .atZone(zone)
            .toLocalDate();
        ZonedDateTime end = localDate.atTime(LocalTime.MAX).atZone(zone);
        return end.toInstant().toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
