package com.drumigo.mobile.ui.history;

import android.app.DatePickerDialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.AdminApiService;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.DriverApiService;
import com.drumigo.mobile.data.api.PassengerApiService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.favorite.FavoriteRouteResponse;
import com.drumigo.mobile.data.model.history.DriverRideHistoryItemResponse;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.history.PassengerRideHistoryItemResponse;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.session.SessionManager;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideHistoryActivity extends AppCompatActivity
    implements RideHistoryAdapter.OnRideClickListener, RideHistoryAdapter.OnFavoriteToggleListener {

    private static final String TAG = "RideHistoryActivity";
    private static final String ROLE_DRIVER = "DRIVER";
    private static final String ROLE_PASSENGER = "PASSENGER";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT = "requestedAt,desc";
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.3F;
    private static final long SHAKE_SLOP_TIME_MS = 800L;

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private TextView fromDateText;
    private TextView toDateText;
    private TextView resultsCountText;
    private Spinner sortSpinner;
    private Button applyFilterButton;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    private Calendar fromDate;
    private Calendar toDate;
    private SimpleDateFormat dateFormat;

    private DriverApiService driverApiService;
    private PassengerApiService passengerApiService;
    private AdminApiService adminApiService;
    private SessionManager sessionManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeAtMs = 0L;
    private boolean shakeSortDescending = true;

    private String currentRole = ROLE_DRIVER;
    private final List<Ride> fetchedRides = new ArrayList<>();
    private SortOption selectedSort = SortOption.NEWEST_FIRST;

    private enum SortOption {
        NEWEST_FIRST,
        OLDEST_FIRST,
        AMOUNT_DESC,
        AMOUNT_ASC,
        STATUS_ASC
    }

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event == null || event.values == null || event.values.length < 3) {
                return;
            }
            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
            if (gForce < SHAKE_THRESHOLD_GRAVITY) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastShakeAtMs < SHAKE_SLOP_TIME_MS) {
                return;
            }
            lastShakeAtMs = now;
            toggleDateSortByShake();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_ride_history);

            dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            fromDate = Calendar.getInstance();
            fromDate.set(2024, Calendar.JANUARY, 1);
            toDate = Calendar.getInstance();

            setupToolbar();
            initializeViews();

            sessionManager = SessionManager.getInstance(this);
            if (!sessionManager.isAuthenticated()) {
                Toast.makeText(this, "Please sign in to view ride history.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
            currentRole = resolveCurrentRole();
            driverApiService = ApiClient.getDriverApiService();
            passengerApiService = ApiClient.getPassengerApiService();
            adminApiService = ApiClient.getAdminApiService();

            setupTabs();
            setupRecyclerView();
            setupDatePickers();
            setupSortControl();

            loadRideHistory();
        } catch (Exception e) {
            Log.e(TAG, "Error while opening ride history", e);
            Toast.makeText(this, "Unable to open ride history", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(
                shakeListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            );
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
        super.onPause();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            Log.w(TAG, "Toolbar not found in layout");
            return;
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRides);
        fromDateText = findViewById(R.id.fromDateText);
        toDateText = findViewById(R.id.toDateText);
        resultsCountText = findViewById(R.id.resultsCountText);
        sortSpinner = findViewById(R.id.sortSpinner);
        applyFilterButton = findViewById(R.id.applyFilterButton);
        tabLayout = findViewById(R.id.rideHistoryTabLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshRideHistory);

        if (fromDateText != null) {
            fromDateText.setText(dateFormat.format(fromDate.getTime()));
        }
        if (toDateText != null) {
            toDateText.setText(dateFormat.format(toDate.getTime()));
        }

        if (applyFilterButton != null) {
            applyFilterButton.setOnClickListener(v -> loadRideHistory());
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadRideHistory);
        }
    }

    private void setupTabs() {
        if (tabLayout == null) {
            return;
        }
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(R.string.ride_history_tab_passenger));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.ride_history_tab_driver));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.ride_history_tab_admin));

        int initialTabIndex = roleToTabIndex(currentRole);
        TabLayout.Tab initialTab = tabLayout.getTabAt(initialTabIndex);
        if (initialTab != null) {
            initialTab.select();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                currentRole = tabIndexToRole(position);
                setupRecyclerView();
                loadRideHistory();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private static int roleToTabIndex(String role) {
        if (ROLE_PASSENGER.equals(role)) return 0;
        if (ROLE_DRIVER.equals(role)) return 1;
        if (ROLE_ADMIN.equals(role)) return 2;
        return 1;
    }

    private static String tabIndexToRole(int index) {
        if (index == 0) return ROLE_PASSENGER;
        if (index == 1) return ROLE_DRIVER;
        if (index == 2) return ROLE_ADMIN;
        return ROLE_DRIVER;
    }

    private void setupRecyclerView() {
        boolean showFavorite = ROLE_PASSENGER.equals(currentRole);
        adapter = new RideHistoryAdapter(
            new ArrayList<>(),
            this,
            this,
            showFavorite,
            showFavorite ? this : null
        );
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupDatePickers() {
        if (fromDateText != null) {
            fromDateText.setOnClickListener(v -> showDatePicker(true));
        }
        if (toDateText != null) {
            toDateText.setOnClickListener(v -> showDatePicker(false));
        }
    }

    private void setupSortControl() {
        if (sortSpinner == null) {
            return;
        }
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.ride_history_sort_options,
            android.R.layout.simple_spinner_item
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setSelection(0);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedSort = mapSortOption(position);
                renderSortedResults();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSort = SortOption.NEWEST_FIRST;
                renderSortedResults();
            }
        });
    }

    private SortOption mapSortOption(int position) {
        if (position == 1) {
            return SortOption.OLDEST_FIRST;
        }
        if (position == 2) {
            return SortOption.AMOUNT_DESC;
        }
        if (position == 3) {
            return SortOption.AMOUNT_ASC;
        }
        if (position == 4) {
            return SortOption.STATUS_ASC;
        }
        return SortOption.NEWEST_FIRST;
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar target = isFromDate ? fromDate : toDate;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            0,
            (view, year, month, dayOfMonth) -> {
                target.set(year, month, dayOfMonth);
                if (isFromDate && fromDateText != null) {
                    fromDateText.setText(dateFormat.format(target.getTime()));
                }
                if (!isFromDate && toDateText != null) {
                    toDateText.setText(dateFormat.format(target.getTime()));
                }
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void loadRideHistory() {
        if (ROLE_DRIVER.equals(currentRole)) {
            loadDriverRideHistory();
            return;
        }
        if (ROLE_PASSENGER.equals(currentRole)) {
            loadPassengerRideHistory();
            return;
        }
        if (ROLE_ADMIN.equals(currentRole)) {
            loadAdminRideHistory();
            return;
        }

        showHistoryLoadError();
        updateHistoryResults(Collections.emptyList());
    }

    private void loadDriverRideHistory() {
        if (driverApiService == null) {
            showHistoryLoadError();
            return;
        }

        long driverId = sessionManager == null ? -1L : sessionManager.getUserId();
        if (driverId <= 0) {
            showHistoryLoadError();
            updateHistoryResults(Collections.emptyList());
            return;
        }

        loadDriverRideHistoryPage(
            driverId,
            toIsoStartOfDay(fromDate),
            toIsoEndOfDay(toDate),
            DEFAULT_PAGE,
            new ArrayList<>()
        );
    }

    private void loadDriverRideHistoryPage(
        long driverId,
        String from,
        String to,
        int page,
        List<Ride> accumulated
    ) {
        driverApiService.getDriverRideHistory(driverId, from, to, page, DEFAULT_PAGE_SIZE, DEFAULT_SORT)
            .enqueue(new Callback<PageResponse<DriverRideHistoryItemResponse>>() {
                @Override
                public void onResponse(
                    @NonNull Call<PageResponse<DriverRideHistoryItemResponse>> call,
                    @NonNull Response<PageResponse<DriverRideHistoryItemResponse>> response
                ) {
                    if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                        showHistoryLoadError();
                        updateHistoryResults(Collections.emptyList());
                        stopRefresh();
                        return;
                    }

                    for (DriverRideHistoryItemResponse item : response.body().content) {
                        Ride mapped = DriverRideHistoryMapper.toRide(item);
                        if (mapped != null) {
                            accumulated.add(mapped);
                        }
                    }

                    PageResponse<DriverRideHistoryItemResponse> body = response.body();
                    int nextPage = body.number + 1;
                    if (nextPage < body.totalPages) {
                        loadDriverRideHistoryPage(driverId, from, to, nextPage, accumulated);
                        return;
                    }

                    updateHistoryResults(accumulated);
                }

                @Override
                public void onFailure(@NonNull Call<PageResponse<DriverRideHistoryItemResponse>> call, @NonNull Throwable t) {
                    showHistoryLoadError();
                    updateHistoryResults(Collections.emptyList());
                    stopRefresh();
                }
            });
    }

    private void loadPassengerRideHistory() {
        if (passengerApiService == null) {
            showHistoryLoadError();
            return;
        }

        long passengerId = sessionManager == null ? -1L : sessionManager.getUserId();
        if (passengerId <= 0) {
            showHistoryLoadError();
            updateHistoryResults(Collections.emptyList());
            return;
        }

        loadPassengerRideHistoryPage(
            passengerId,
            toIsoStartOfDay(fromDate),
            toIsoEndOfDay(toDate),
            DEFAULT_PAGE,
            new ArrayList<>()
        );
    }

    private void loadPassengerRideHistoryPage(
        long passengerId,
        String from,
        String to,
        int page,
        List<Ride> accumulated
    ) {
        passengerApiService.getPassengerRideHistory(passengerId, from, to, page, DEFAULT_PAGE_SIZE, DEFAULT_SORT)
            .enqueue(new Callback<PageResponse<PassengerRideHistoryItemResponse>>() {
                @Override
                public void onResponse(
                    @NonNull Call<PageResponse<PassengerRideHistoryItemResponse>> call,
                    @NonNull Response<PageResponse<PassengerRideHistoryItemResponse>> response
                ) {
                    if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                        showHistoryLoadError();
                        updateHistoryResults(Collections.emptyList());
                        stopRefresh();
                        return;
                    }

                    for (PassengerRideHistoryItemResponse item : response.body().content) {
                        Ride mapped = PassengerRideHistoryMapper.toRide(item);
                        if (mapped != null) {
                            accumulated.add(mapped);
                        }
                    }

                    PageResponse<PassengerRideHistoryItemResponse> body = response.body();
                    int nextPage = body.number + 1;
                    if (nextPage < body.totalPages) {
                        loadPassengerRideHistoryPage(passengerId, from, to, nextPage, accumulated);
                        return;
                    }

                    updateHistoryResults(accumulated);
                }

                @Override
                public void onFailure(@NonNull Call<PageResponse<PassengerRideHistoryItemResponse>> call, @NonNull Throwable t) {
                    showHistoryLoadError();
                    updateHistoryResults(Collections.emptyList());
                    stopRefresh();
                }
            });
    }

    private void loadAdminRideHistory() {
        if (adminApiService == null) {
            showHistoryLoadError();
            return;
        }

        loadAdminRideHistoryPage(
            toIsoStartOfDay(fromDate),
            toIsoEndOfDay(toDate),
            DEFAULT_PAGE,
            new ArrayList<>()
        );
    }

    private void loadAdminRideHistoryPage(
        String from,
        String to,
        int page,
        List<Ride> accumulated
    ) {
        adminApiService.getAdminRideHistory(from, to, page, DEFAULT_PAGE_SIZE, DEFAULT_SORT)
            .enqueue(new Callback<PageResponse<RideResponse>>() {
                @Override
                public void onResponse(
                    @NonNull Call<PageResponse<RideResponse>> call,
                    @NonNull Response<PageResponse<RideResponse>> response
                ) {
                    if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                        showHistoryLoadError();
                        updateHistoryResults(Collections.emptyList());
                        stopRefresh();
                        return;
                    }

                    for (RideResponse item : response.body().content) {
                        Ride mapped = AdminRideHistoryMapper.toRide(item);
                        if (mapped != null) {
                            accumulated.add(mapped);
                        }
                    }

                    PageResponse<RideResponse> body = response.body();
                    int nextPage = body.number + 1;
                    if (nextPage < body.totalPages) {
                        loadAdminRideHistoryPage(from, to, nextPage, accumulated);
                        return;
                    }

                    updateHistoryResults(accumulated);
                }

                @Override
                public void onFailure(@NonNull Call<PageResponse<RideResponse>> call, @NonNull Throwable t) {
                    showHistoryLoadError();
                    updateHistoryResults(Collections.emptyList());
                }
            });
    }

    private void stopRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateHistoryResults(List<Ride> rides) {
        fetchedRides.clear();
        if (rides != null) {
            fetchedRides.addAll(rides);
        }
        renderSortedResults();
        stopRefresh();
    }

    private void renderSortedResults() {
        List<Ride> sorted = new ArrayList<>(fetchedRides);
        sorted.sort(buildComparator(selectedSort));

        if (adapter != null) {
            adapter.updateRides(sorted);
        }
        if (resultsCountText != null) {
            resultsCountText.setText(getString(R.string.ride_history_results_count, sorted.size()));
        }
    }

    private void toggleDateSortByShake() {
        shakeSortDescending = !shakeSortDescending;
        selectedSort = shakeSortDescending ? SortOption.NEWEST_FIRST : SortOption.OLDEST_FIRST;
        if (sortSpinner != null) {
            sortSpinner.setSelection(shakeSortDescending ? 0 : 1);
        }
        renderSortedResults();
        Toast.makeText(
            this,
            shakeSortDescending ? getString(R.string.ride_history_sort_newest) : getString(R.string.ride_history_sort_oldest),
            Toast.LENGTH_SHORT
        ).show();
    }

    private Comparator<Ride> buildComparator(SortOption option) {
        Comparator<Ride> tieBreaker = (a, b) -> Long.compare(
            b == null ? 0L : b.getSortTimestampEpochMs(),
            a == null ? 0L : a.getSortTimestampEpochMs()
        );

        if (option == SortOption.OLDEST_FIRST) {
            return Comparator.comparingLong(ride -> ride == null ? 0L : ride.getSortTimestampEpochMs());
        }
        if (option == SortOption.AMOUNT_DESC) {
            return (a, b) -> {
                int amountCompare = Double.compare(
                    b == null ? 0.0d : b.getAmountValue(),
                    a == null ? 0.0d : a.getAmountValue()
                );
                return amountCompare != 0 ? amountCompare : tieBreaker.compare(a, b);
            };
        }
        if (option == SortOption.AMOUNT_ASC) {
            return (a, b) -> {
                int amountCompare = Double.compare(
                    a == null ? 0.0d : a.getAmountValue(),
                    b == null ? 0.0d : b.getAmountValue()
                );
                return amountCompare != 0 ? amountCompare : tieBreaker.compare(a, b);
            };
        }
        if (option == SortOption.STATUS_ASC) {
            return (a, b) -> {
                String statusA = a == null || a.getStatus() == null ? "" : a.getStatus();
                String statusB = b == null || b.getStatus() == null ? "" : b.getStatus();
                int statusCompare = statusA.compareToIgnoreCase(statusB);
                return statusCompare != 0 ? statusCompare : tieBreaker.compare(a, b);
            };
        }

        return tieBreaker;
    }

    private void showHistoryLoadError() {
        Toast.makeText(this, "Failed to load ride history", Toast.LENGTH_SHORT).show();
    }

    private String resolveCurrentRole() {
        if (sessionManager == null) {
            return ROLE_DRIVER;
        }
        String role = sessionManager.getRole();
        if (role == null || role.trim().isEmpty()) {
            return ROLE_DRIVER;
        }
        return role.trim().toUpperCase(Locale.ENGLISH);
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
    public void onRideSelected(Ride ride) {
        if (ride == null) {
            return;
        }
        boolean showRating = ROLE_PASSENGER.equals(currentRole);
        RideApiService rideApiService = showRating ? ApiClient.getRideApiService() : null;
        RideHistoryDetailsBottomSheet.show(this, ride, showRating, rideApiService);
    }

    @Override
    public void onFavoriteToggled(Ride ride) {
        if (ride == null || passengerApiService == null || sessionManager == null) {
            return;
        }
        long passengerId = sessionManager.getUserId();
        if (passengerId <= 0) {
            Toast.makeText(this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Long rideId = ride.getId();
        if (rideId == null) {
            return;
        }
        if (ride.isFavorite()) {
            passengerApiService.deleteFavoriteRouteByRide(passengerId, rideId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        updateRideFavoriteState(ride.getId(), false, null);
                        Toast.makeText(RideHistoryActivity.this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RideHistoryActivity.this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(RideHistoryActivity.this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            passengerApiService.createFavoriteRouteFromRide(passengerId, rideId).enqueue(new Callback<FavoriteRouteResponse>() {
                @Override
                public void onResponse(
                    @NonNull Call<FavoriteRouteResponse> call,
                    @NonNull Response<FavoriteRouteResponse> response
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        updateRideFavoriteState(ride.getId(), true, response.body().id);
                        Toast.makeText(RideHistoryActivity.this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RideHistoryActivity.this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(
                    @NonNull Call<FavoriteRouteResponse> call,
                    @NonNull Throwable t
                ) {
                    Toast.makeText(RideHistoryActivity.this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateRideFavoriteState(Long rideId, boolean favorite, Long favoriteRouteId) {
        for (int i = 0; i < fetchedRides.size(); i++) {
            Ride r = fetchedRides.get(i);
            if (r != null && rideId.equals(r.getId())) {
                r.setFavorite(favorite);
                r.setFavoriteRouteId(favoriteRouteId);
                adapter.updateRides(new ArrayList<>(fetchedRides));
                break;
            }
        }
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
