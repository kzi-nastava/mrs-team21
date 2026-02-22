package com.drumigo.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.drumigo.mobile.databinding.ActivityMainBinding;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.DriverApiService;
import com.drumigo.mobile.data.api.NotificationApiService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.DriverLocationUpdateRequest;
import com.drumigo.mobile.data.model.NotificationResponse;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.ride.ActiveRideIdResponse;
import com.drumigo.mobile.session.SessionManager;
import com.drumigo.mobile.ui.ride.RideTrackingConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String ROLE_PASSENGER = "PASSENGER";
    private static final String ROLE_DRIVER = "DRIVER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final long DRIVER_LOCATION_PING_INTERVAL_MS = 5_000L;
    private static final long ADMIN_NOTIFICATION_POLLING_INTERVAL_MS = 8_000L;
    private static final int ADMIN_NOTIFICATION_PAGE_SIZE = 20;
    private static final String ADMIN_NOTIFICATION_SORT = "createdAt,desc";
    private static final String PANIC_NOTIFICATION_TYPE = "PANIC_ALERT";
    private static final String PANIC_NOTIFICATION_CHANNEL_ID = "panic_alerts";

    private ActivityMainBinding binding;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private ImageView toolbarLandingLogo;
    private View toolbarAuthActions;
    private MaterialButton toolbarSignInButton;
    private MaterialButton toolbarSignUpButton;
    private RideApiService rideApiService;
    private DriverApiService driverApiService;
    private NotificationApiService notificationApiService;
    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private long lastSeenAdminPanicNotificationId = -1L;
    private final Handler ridePollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable ridePollingRunnable = new Runnable() {
        @Override
        public void run() {
            checkActiveRides();
            ridePollingHandler.postDelayed(this, 10_000L);
        }
    };
    private final Handler driverLocationPingHandler = new Handler(Looper.getMainLooper());
    private final Runnable driverLocationPingRunnable = new Runnable() {
        @Override
        public void run() {
            pingDriverLocationOnce();
            driverLocationPingHandler.postDelayed(this, DRIVER_LOCATION_PING_INTERVAL_MS);
        }
    };
    private final Handler adminNotificationPollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable adminNotificationPollingRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAdminNotificationsOnce();
            adminNotificationPollingHandler.postDelayed(this, ADMIN_NOTIFICATION_POLLING_INTERVAL_MS);
        }
    };
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
            boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
            if (fineGranted || coarseGranted) {
                startDriverLocationPings();
            } else {
                Log.w(TAG, "Location permission denied. Driver location pings are disabled.");
            }
        });
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (Boolean.TRUE.equals(granted)) {
                startAdminPanicNotificationPollingIfNeeded();
            } else {
                Log.w(TAG, "Notification permission denied. Admin panic alerts will not appear as system notifications.");
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupEdgeToEdge();

        setupToolbar();

        setupNavigation();
        
        setupDrawer();

        setupBackPressedHandler();

        rideApiService = ApiClient.getRideApiService();
        driverApiService = ApiClient.getDriverApiService();
        notificationApiService = ApiClient.getNotificationApiService();
        sessionManager = SessionManager.getInstance(this);
        if (savedInstanceState == null) {
            sessionManager.clearSessionOnColdStartIfNeeded();
        }
        sessionManager.getToken();
        updateDrawerMenuForCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showSessionExpiredNoticeIfNeeded();
        updateToolbarForDestination(getCurrentDestinationId());
        updateDrawerMenuForCurrentUser();
        updateDrawerAvailabilityForCurrentUser();
        updateToolbarAuthActions(getCurrentDestinationId());
        enforceAuthenticationForProtectedDestination();
        startDriverLocationPingsIfNeeded();
        startAdminPanicNotificationPollingIfNeeded();
    }

    @Override
    protected void onStop() {
        stopDriverLocationPings();
        stopAdminPanicNotificationPolling();
        super.onStop();
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attrs);
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            View toolbarView = binding.main.findViewById(R.id.toolbar);
            if (toolbarView != null) {
                // Padding so toolbar content is below status bar and clear of cutout
                toolbarView.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    toolbarView.getPaddingBottom()
                );
                TypedValue tv = new TypedValue();
                if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                    int actionBarSize = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
                    int totalHeight = actionBarSize + systemBars.top;
                    toolbarView.setMinimumHeight(totalHeight);
                    // Force toolbar to actually use that height (parent is LinearLayout)
                    ViewGroup.LayoutParams lp = toolbarView.getLayoutParams();
                    if (lp != null) {
                        lp.height = totalHeight;
                        toolbarView.setLayoutParams(lp);
                    }
                }
            }
            return insets;
        });
    }

    private void setupToolbar() {
        toolbar = binding.getRoot().findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("");
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);

        toolbarLandingLogo = new ImageView(this);
        toolbarLandingLogo.setImageResource(R.drawable.ic_logo_white);
        toolbarLandingLogo.setScaleType(ImageView.ScaleType.FIT_START);
        Toolbar.LayoutParams logoLayoutParams = new Toolbar.LayoutParams(
            dpToPx(120),
            dpToPx(28),
            Gravity.START | Gravity.CENTER_VERTICAL
        );
        logoLayoutParams.setMarginStart(dpToPx(8));
        toolbar.addView(toolbarLandingLogo, logoLayoutParams);
        toolbarLandingLogo.setVisibility(View.GONE);

        toolbarAuthActions = getLayoutInflater().inflate(R.layout.toolbar_auth_actions, toolbar, false);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT,
            Gravity.END | Gravity.CENTER_VERTICAL
        );
        layoutParams.setMarginEnd(dpToPx(12));
        toolbar.addView(toolbarAuthActions, layoutParams);
        toolbarSignInButton = toolbarAuthActions.findViewById(R.id.toolbarBtnSignIn);
        toolbarSignUpButton = toolbarAuthActions.findViewById(R.id.toolbarBtnSignUp);

        toolbarSignInButton.setOnClickListener(v -> navigateToDestination(R.id.loginFragment));
        toolbarSignUpButton.setOnClickListener(v -> navigateToDestination(R.id.registrationFragment));
        toolbarAuthActions.setVisibility(View.GONE);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                updateToolbarForDestination(destination.getId());
                updateDrawerMenuForCurrentUser();
                updateDrawerAvailabilityForCurrentUser();
                updateSelectedNavItem(destination.getId());
                updateToolbarAuthActions(destination.getId());
                enforceAuthenticationForProtectedDestination();
            });
        }
    }

    private void setupDrawer() {
        drawerLayout = binding.drawerLayout;
        binding.navigationView.setNavigationItemSelectedListener(this);
        updateDrawerMenuForCurrentUser();
        updateDrawerAvailabilityForCurrentUser();
        
        View headerView = binding.navigationView.getHeaderView(0);
        if (headerView != null) {
            int nameId = getResources().getIdentifier("navHeaderName", "id", getPackageName());
            int emailId = getResources().getIdentifier("navHeaderEmail", "id", getPackageName());

            if (nameId != 0) {
                View v = headerView.findViewById(nameId);
                if (v instanceof TextView) {
                    ((TextView) v).setText(R.string.nav_header_guest);
                }
            }

            if (emailId != 0) {
                View v = headerView.findViewById(emailId);
                if (v instanceof TextView) {
                    ((TextView) v).setText(R.string.nav_header_guest_email);
                }
            }
        }
    }

    private void updateDrawerMenuForCurrentUser() {
        if (binding == null) {
            return;
        }
        Menu menu = binding.navigationView.getMenu();
        if (menu == null) {
            return;
        }

        hideAllDrawerItems(menu);

        if (!isUserAuthenticated()) {
            setDrawerItemState(menu, R.id.nav_home, true, true);
            setDrawerItemState(menu, R.id.nav_login, true, true);
            setDrawerItemState(menu, R.id.nav_registration, true, true);
            setDrawerItemState(menu, R.id.nav_logout, false, false);
            return;
        }

        String role = getCurrentRoleNormalized();
        if (ROLE_DRIVER.equals(role)) {
            setDrawerItemState(menu, R.id.nav_ride_tracking, true, true);
            setDrawerItemState(menu, R.id.nav_ride_history, true, true);
            setDrawerItemState(menu, R.id.nav_profile, true, true);
            setDrawerItemState(menu, R.id.nav_support, true, false);
            setDrawerItemState(menu, R.id.nav_logout, true, true);
            return;
        }

        if (ROLE_ADMIN.equals(role)) {
            // Mirrors web admin navbar placeholders.
            setDrawerItemState(menu, R.id.nav_dashboard, true, false);
            setDrawerItemState(menu, R.id.nav_active_rides_admin, true, false);
            setDrawerItemState(menu, R.id.nav_ride_history, true, true);
            setDrawerItemState(menu, R.id.nav_panic_notifications, true, false);
            setDrawerItemState(menu, R.id.nav_live_support, true, false);
            setDrawerItemState(menu, R.id.nav_register_driver, true, false);
            setDrawerItemState(menu, R.id.nav_drivers, true, false);
            setDrawerItemState(menu, R.id.nav_passengers, true, false);
            setDrawerItemState(menu, R.id.nav_reports, true, false);
            setDrawerItemState(menu, R.id.nav_all_notifications, true, false);
            setDrawerItemState(menu, R.id.nav_profile, true, true);
            setDrawerItemState(menu, R.id.nav_logout, true, true);
            return;
        }

        // Default authenticated role: passenger.
        setDrawerItemState(menu, R.id.nav_order_ride, true, true);
        setDrawerItemState(menu, R.id.nav_ride_tracking, true, true);
        setDrawerItemState(menu, R.id.nav_ride_history, true, true);
        setDrawerItemState(menu, R.id.nav_profile, true, true);
        setDrawerItemState(menu, R.id.nav_support, true, false);
        setDrawerItemState(menu, R.id.nav_logout, true, true);
    }

    private void updateDrawerAvailabilityForCurrentUser() {
        if (drawerLayout == null || toolbar == null) {
            return;
        }
        if (isUserAuthenticated()) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
            setNavigationIconLarge(toolbar, R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(v -> openDrawer());
            toolbar.setNavigationContentDescription(R.string.nav_drawer_open);
            return;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
    }

    private void hideAllDrawerItems(Menu menu) {
        int[] allItemIds = new int[] {
            R.id.nav_home,
            R.id.nav_login,
            R.id.nav_registration,
            R.id.nav_order_ride,
            R.id.nav_ride_tracking,
            R.id.nav_ride_history,
            R.id.nav_dashboard,
            R.id.nav_active_rides_admin,
            R.id.nav_panic_notifications,
            R.id.nav_live_support,
            R.id.nav_register_driver,
            R.id.nav_drivers,
            R.id.nav_passengers,
            R.id.nav_reports,
            R.id.nav_all_notifications,
            R.id.nav_profile,
            R.id.nav_support,
            R.id.nav_logout
        };
        for (int itemId : allItemIds) {
            setDrawerItemState(menu, itemId, false, false);
        }
    }

    private void setDrawerItemState(Menu menu, int itemId, boolean visible, boolean enabled) {
        MenuItem menuItem = menu.findItem(itemId);
        if (menuItem == null) {
            return;
        }
        menuItem.setVisible(visible);
        menuItem.setEnabled(enabled);
        if (!visible || !enabled) {
            menuItem.setChecked(false);
        }
    }

    private void updateSelectedNavItem(int destinationId) {
        if (destinationId == -1) {
            return;
        }
        int menuItemId;
        if (destinationId == R.id.landingFragment || destinationId == R.id.activeVehiclesMapFragment) {
            menuItemId = isPassengerAuthenticated() ? R.id.nav_order_ride : R.id.nav_home;
        } else if (destinationId == R.id.loginFragment) {
            menuItemId = R.id.nav_login;
        } else if (destinationId == R.id.registrationFragment) {
            menuItemId = R.id.nav_registration;
        } else if (destinationId == R.id.profileFragment) {
            menuItemId = R.id.nav_profile;
        } else if (destinationId == R.id.rideTrackingFragment) {
            menuItemId = R.id.nav_ride_tracking;
        } else {
            return;
        }
        Menu menu = binding.navigationView.getMenu();
        MenuItem menuItem = menu.findItem(menuItemId);
        if (menuItem != null && menuItem.isVisible() && menuItem.isEnabled()) {
            binding.navigationView.setCheckedItem(menuItemId);
        }
    }

    private void updateToolbarAuthActions(int destinationId) {
        if (toolbarAuthActions == null) {
            return;
        }
        boolean show = destinationId == R.id.landingFragment && !isUserAuthenticated();
        toolbarAuthActions.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateToolbarForDestination(int destinationId) {
        if (toolbar == null || destinationId == -1) {
            return;
        }
        if (destinationId == R.id.loginFragment || destinationId == R.id.registrationFragment) {
            toolbar.setVisibility(View.GONE);
            return;
        }

        toolbar.setVisibility(View.VISIBLE);
        if (destinationId == R.id.profileFragment) {
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(true);
            toolbar.setTitle(R.string.profile_title);
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setTitle("");
        }
        if (destinationId == R.id.landingFragment && !isUserAuthenticated()) {
            if (toolbarLandingLogo != null) {
                toolbarLandingLogo.setVisibility(View.VISIBLE);
            }
        } else {
            if (toolbarLandingLogo != null) {
                toolbarLandingLogo.setVisibility(View.GONE);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /** Set a larger navigation icon (32dp) so the menu is easier to tap and see. */
    private void setNavigationIconLarge(MaterialToolbar toolbar, int drawableResId) {
        Drawable d = AppCompatResources.getDrawable(this, drawableResId);
        if (d != null) {
            d = d.mutate();
            d.setTint(android.graphics.Color.WHITE);
            int size = dpToPx(32);
            d.setBounds(0, 0, size, size);
            toolbar.setNavigationIcon(d);
        } else {
            toolbar.setNavigationIcon(drawableResId);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (!item.isEnabled()) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            navController.navigate(R.id.landingFragment);
        } else if (itemId == R.id.nav_order_ride) {
            navController.navigate(R.id.landingFragment);
        } else if (itemId == R.id.nav_login) {
            navController.navigate(R.id.loginFragment);
        } else if (itemId == R.id.nav_registration) {
            navController.navigate(R.id.registrationFragment);
        } else if (itemId == R.id.nav_logout) {
            clearAuthState();
            stopDriverLocationPings();
            navController.navigate(R.id.loginFragment);
        } else if (itemId == R.id.nav_ride_history) {
            if (isUserAuthenticated()) {
                Intent intent = new Intent(this, com.drumigo.mobile.ui.history.RideHistoryActivity.class);
                startActivity(intent);
            }
        } else if (itemId == R.id.nav_ride_tracking) {
            if (isUserAuthenticated()) {
                navigateToDestination(R.id.rideTrackingFragment);
            }
        } else if (itemId == R.id.nav_profile) {
            navController.navigate(R.id.profileFragment);
        } else {
            android.widget.Toast.makeText(
                this,
                R.string.nav_not_implemented,
                android.widget.Toast.LENGTH_SHORT
            ).show();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openDrawer() {
        if (drawerLayout != null
            && drawerLayout.getDrawerLockMode(GravityCompat.START) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public boolean isDrawerOpen() {
        return drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public NavController getNavController() {
        return navController;
    }

    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Close drawer on back press if open
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Let default back behavior happen
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void startRideAutoTracking() {
        if (!isUserAuthenticated()) {
            stopRideAutoTracking();
            return;
        }
        ridePollingHandler.removeCallbacks(ridePollingRunnable);
        ridePollingHandler.post(ridePollingRunnable);
    }

    private void stopRideAutoTracking() {
        ridePollingHandler.removeCallbacks(ridePollingRunnable);
    }

    private void startDriverLocationPingsIfNeeded() {
        if (!isLoggedInDriver()) {
            stopDriverLocationPings();
            return;
        }
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        startDriverLocationPings();
    }

    private void startDriverLocationPings() {
        if (!isLoggedInDriver() || !hasLocationPermission()) {
            return;
        }
        driverLocationPingHandler.removeCallbacks(driverLocationPingRunnable);
        driverLocationPingHandler.post(driverLocationPingRunnable);
    }

    private void stopDriverLocationPings() {
        driverLocationPingHandler.removeCallbacks(driverLocationPingRunnable);
    }

    private boolean isLoggedInDriver() {
        return isDriverAuthenticated();
    }

    private String getAuthToken() {
        if (sessionManager == null) {
            return null;
        }
        String token = sessionManager.getToken();
        return (token == null || token.isBlank()) ? null : token;
    }

    private boolean isUserAuthenticated() {
        return getAuthToken() != null;
    }

    private boolean isPassengerAuthenticated() {
        return isUserAuthenticated() && ROLE_PASSENGER.equals(getCurrentRoleNormalized());
    }

    private boolean isDriverAuthenticated() {
        return isUserAuthenticated() && ROLE_DRIVER.equals(getCurrentRoleNormalized());
    }

    private boolean isAdminAuthenticated() {
        return isUserAuthenticated() && ROLE_ADMIN.equals(getCurrentRoleNormalized());
    }

    private String getCurrentRoleNormalized() {
        if (sessionManager == null) {
            return "";
        }
        String role = sessionManager.getRole();
        if (role == null) {
            return "";
        }
        return role.trim().toUpperCase();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        locationPermissionLauncher.launch(new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @SuppressLint("MissingPermission")
    private void pingDriverLocationOnce() {
        if (!isLoggedInDriver() || !hasLocationPermission() || fusedLocationClient == null) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    sendDriverLocationPing(location);
                } else {
                    requestCurrentLocationAndPing();
                }
            })
            .addOnFailureListener(error ->
                Log.w(TAG, "Failed to read last known location for driver ping.", error)
            );
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocationAndPing() {
        if (fusedLocationClient == null) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    sendDriverLocationPing(location);
                }
            })
            .addOnFailureListener(error ->
                Log.w(TAG, "Failed to fetch current location for driver ping.", error)
            );
    }

    private void sendDriverLocationPing(Location location) {
        if (driverApiService == null) {
            return;
        }

        DriverLocationUpdateRequest request =
            new DriverLocationUpdateRequest(location.getLatitude(), location.getLongitude());

        driverApiService.updateMyLocation(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Driver location ping failed with HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.w(TAG, "Driver location ping request failed.", t);
            }
        });
    }

    private void startAdminPanicNotificationPollingIfNeeded() {
        if (!isAdminAuthenticated()) {
            stopAdminPanicNotificationPolling();
            return;
        }
        ensurePanicNotificationChannel();
        if (!hasNotificationPermission()) {
            requestNotificationPermissionIfNeeded();
            return;
        }
        adminNotificationPollingHandler.removeCallbacks(adminNotificationPollingRunnable);
        adminNotificationPollingHandler.post(adminNotificationPollingRunnable);
    }

    private void stopAdminPanicNotificationPolling() {
        adminNotificationPollingHandler.removeCallbacks(adminNotificationPollingRunnable);
    }

    private void fetchAdminNotificationsOnce() {
        if (!isAdminAuthenticated() || notificationApiService == null || sessionManager == null) {
            return;
        }
        long userId = sessionManager.getUserId();
        if (userId <= 0L) {
            return;
        }

        notificationApiService.getUserNotifications(
            userId,
            0,
            ADMIN_NOTIFICATION_PAGE_SIZE,
            ADMIN_NOTIFICATION_SORT
        ).enqueue(new Callback<PageResponse<NotificationResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<PageResponse<NotificationResponse>> call,
                @NonNull Response<PageResponse<NotificationResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                    return;
                }

                List<NotificationResponse> notifications = response.body().content;
                long maxPanicId = lastSeenAdminPanicNotificationId;
                for (NotificationResponse item : notifications) {
                    if (item == null || item.id == null || !PANIC_NOTIFICATION_TYPE.equals(item.type)) {
                        continue;
                    }
                    maxPanicId = Math.max(maxPanicId, item.id);
                }

                // Initialize watermark on first successful fetch to avoid replaying historical alerts.
                if (lastSeenAdminPanicNotificationId < 0L) {
                    lastSeenAdminPanicNotificationId = Math.max(maxPanicId, 0L);
                    return;
                }

                long previousSeen = lastSeenAdminPanicNotificationId;
                for (int i = notifications.size() - 1; i >= 0; i--) {
                    NotificationResponse item = notifications.get(i);
                    if (item == null || item.id == null || !PANIC_NOTIFICATION_TYPE.equals(item.type)) {
                        continue;
                    }
                    if (item.id > previousSeen) {
                        showPanicSystemNotification(item);
                    }
                }
                lastSeenAdminPanicNotificationId = Math.max(lastSeenAdminPanicNotificationId, maxPanicId);
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<NotificationResponse>> call, @NonNull Throwable t) {
                Log.w(TAG, "Failed to fetch admin notifications.", t);
            }
        });
    }

    private void showPanicSystemNotification(@NonNull NotificationResponse notification) {
        if (!hasNotificationPermission()) {
            return;
        }
        String message = notification.message == null || notification.message.trim().isEmpty()
            ? getString(R.string.admin_panic_notification_fallback)
            : notification.message.trim();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
            this,
            notification.id == null ? 0 : notification.id.intValue(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PANIC_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(getString(R.string.admin_panic_notification_title))
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent);

        NotificationManagerCompat.from(this).notify(
            notification.id == null ? (int) System.currentTimeMillis() : notification.id.intValue(),
            builder.build()
        );
    }

    private void ensurePanicNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(PANIC_NOTIFICATION_CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
            PANIC_NOTIFICATION_CHANNEL_ID,
            getString(R.string.admin_panic_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.admin_panic_notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission()) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void clearAuthState() {
        if (sessionManager != null) {
            sessionManager.clearSession();
        }
        stopAdminPanicNotificationPolling();
        lastSeenAdminPanicNotificationId = -1L;
        updateDrawerMenuForCurrentUser();
        updateDrawerAvailabilityForCurrentUser();
        updateToolbarAuthActions(getCurrentDestinationId());
    }

    private void checkActiveRides() {
        if (!isUserAuthenticated()) {
            showSessionExpiredNoticeIfNeeded();
            enforceAuthenticationForProtectedDestination();
            return;
        }
        if (!shouldAutoOpenRideTrackingNow()) {
            return;
        }

        if (RideTrackingConfig.USE_MOCK_UPDATES) {
            navigateToRide(RideTrackingConfig.MOCK_RIDE_ID);
            return;
        }

        if (rideApiService == null) {
            return;
        }
        rideApiService.getMyActiveRide().enqueue(new Callback<ActiveRideIdResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<ActiveRideIdResponse> call,
                @NonNull Response<ActiveRideIdResponse> response
            ) {
                if (!shouldAutoOpenRideTrackingNow()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || response.body().rideId == null) {
                    return;
                }
                navigateToRide(response.body().rideId);
            }

            @Override
            public void onFailure(@NonNull Call<ActiveRideIdResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void navigateToRide(long rideId) {
        if (navController == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("rideId", rideId);
        navController.navigate(R.id.rideTrackingFragment, args);
    }

    private void navigateToDestination(int destinationId) {
        if (navController == null) {
            return;
        }
        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination != null && currentDestination.getId() == destinationId) {
            return;
        }
        navController.navigate(destinationId);
    }

    private int getCurrentDestinationId() {
        if (navController == null || navController.getCurrentDestination() == null) {
            return -1;
        }
        return navController.getCurrentDestination().getId();
    }

    private boolean redirectAuthenticatedUserAwayFromLanding() {
        if (!isUserAuthenticated() || navController == null) {
            return false;
        }
        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination == null || currentDestination.getId() != R.id.landingFragment) {
            return false;
        }
        int targetDestination = getAuthenticatedStartDestination();
        if (targetDestination == currentDestination.getId()) {
            return false;
        }
        navController.navigate(targetDestination);
        return true;
    }

    private int getAuthenticatedStartDestination() {
        String role = getCurrentRoleNormalized();
        if (ROLE_PASSENGER.equals(role)) {
            return R.id.activeVehiclesMapFragment;
        }
        return R.id.profileFragment;
    }

    private boolean isAutoRideTrackingAllowedDestination(int destinationId) {
        return destinationId == R.id.landingFragment
            || destinationId == R.id.activeVehiclesMapFragment;
    }

    private boolean shouldAutoOpenRideTrackingNow() {
        if (navController == null) {
            return false;
        }
        NavDestination current = navController.getCurrentDestination();
        if (current == null) {
            return false;
        }
        int currentDestinationId = current.getId();
        if (currentDestinationId == R.id.rideTrackingFragment) {
            return false;
        }
        return isAutoRideTrackingAllowedDestination(currentDestinationId);
    }

    private void showSessionExpiredNoticeIfNeeded() {
        if (sessionManager != null && sessionManager.consumeSessionExpiredNotice()) {
            android.widget.Toast.makeText(
                this,
                "Your session expired. Please sign in again.",
                android.widget.Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void enforceAuthenticationForProtectedDestination() {
        if (navController == null || sessionManager == null || sessionManager.isAuthenticated()) {
            return;
        }
        NavDestination current = navController.getCurrentDestination();
        if (current == null) {
            return;
        }
        int destinationId = current.getId();
        if (isPublicDestination(destinationId)) {
            return;
        }
        navController.navigate(R.id.loginFragment);
    }

    private boolean isPublicDestination(int destinationId) {
        return destinationId == R.id.landingFragment
            || destinationId == R.id.loginFragment
            || destinationId == R.id.registrationFragment
            || destinationId == R.id.forgotPasswordFragment
            || destinationId == R.id.resetPasswordFragment;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
