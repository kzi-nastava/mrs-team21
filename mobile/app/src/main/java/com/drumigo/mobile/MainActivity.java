package com.drumigo.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.drumigo.mobile.databinding.ActivityMainBinding;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.DriverApiService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.DriverLocationUpdateRequest;
import com.drumigo.mobile.data.model.ride.RideResponse;
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
    private static final String AUTH_PREFS = "auth";
    private static final String ROLE_DRIVER = "DRIVER";
    private static final long DRIVER_LOCATION_PING_INTERVAL_MS = 5_000L;

    private ActivityMainBinding binding;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private View toolbarAuthActions;
    private MaterialButton toolbarSignInButton;
    private MaterialButton toolbarSignUpButton;
    private RideApiService rideApiService;
    private DriverApiService driverApiService;
    private FusedLocationProviderClient fusedLocationClient;
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRideAutoTracking();
        startDriverLocationPingsIfNeeded();
    }

    @Override
    protected void onStop() {
        stopRideAutoTracking();
        stopDriverLocationPings();
        super.onStop();
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupToolbar() {
        toolbar = binding.getRoot().findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Ensure the hamburger icon is visible (set after setSupportActionBar)
        toolbar.setNavigationIcon(R.drawable.ic_menu);

        // Handle navigation icon (hamburger menu) clicks
        toolbar.setNavigationOnClickListener(v -> openDrawer());

        toolbarAuthActions = getLayoutInflater().inflate(R.layout.toolbar_auth_actions, toolbar, false);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT,
            Gravity.END | Gravity.CENTER_VERTICAL
        );
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
                updateSelectedNavItem(destination.getId());
                updateToolbarAuthActions(destination.getId());
            });
        }
    }

    private void setupDrawer() {
        drawerLayout = binding.drawerLayout;
        binding.navigationView.setNavigationItemSelectedListener(this);
        
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

    private void updateSelectedNavItem(int destinationId) {
        int menuItemId;
        if (destinationId == R.id.landingFragment || destinationId == R.id.activeVehiclesMapFragment) {
            menuItemId = R.id.nav_active_vehicles;
        } else if (destinationId == R.id.loginFragment) {
            menuItemId = R.id.nav_login;
        } else if (destinationId == R.id.registrationFragment) {
            menuItemId = R.id.nav_registration;
        } else if (destinationId == R.id.forgotPasswordFragment) {
            menuItemId = R.id.nav_forgot_password;
        } else if (destinationId == R.id.resetPasswordFragment) {
            menuItemId = R.id.nav_reset_password;
        } else if (destinationId == R.id.profileFragment) {
            menuItemId = R.id.nav_profile;
        } else if (destinationId == R.id.rideTrackingFragment) {
            return;
        } else {
            return;
        }
        binding.navigationView.setCheckedItem(menuItemId);
    }

    private void updateToolbarAuthActions(int destinationId) {
        if (toolbarAuthActions == null) {
            return;
        }
        boolean show = destinationId == R.id.landingFragment;
        toolbarAuthActions.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_active_vehicles) {
            navController.navigate(R.id.landingFragment);
        } else if (itemId == R.id.nav_login) {
            navController.navigate(R.id.loginFragment);
        } else if (itemId == R.id.nav_registration) {
            navController.navigate(R.id.registrationFragment);
        } else if (itemId == R.id.nav_forgot_password) {
            navController.navigate(R.id.forgotPasswordFragment);
        } else if (itemId == R.id.nav_reset_password) {
            navController.navigate(R.id.resetPasswordFragment);
        } else if (itemId == R.id.nav_logout) {
            // Handle logout - for now just go to login
            clearAuthState();
            stopDriverLocationPings();
            navController.navigate(R.id.loginFragment);
        } else if (itemId == R.id.nav_ride_history) {
            // Launch RideHistoryActivity
            Intent intent = new Intent(this, com.drumigo.mobile.ui.history.RideHistoryActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_profile) {
            // Navigate to profile fragment
            navController.navigate(R.id.profileFragment);
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openDrawer() {
        if (drawerLayout != null) {
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
        SharedPreferences prefs = getAuthPrefs();
        String token = prefs.getString("token", "");
        String role = prefs.getString("role", "");
        return token != null && !token.isBlank() && ROLE_DRIVER.equalsIgnoreCase(role);
    }

    private SharedPreferences getAuthPrefs() {
        return getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
    }

    private String getAuthToken() {
        String token = getAuthPrefs().getString("token", "");
        if (token == null || token.isBlank()) {
            return null;
        }
        return token;
    }

    private boolean isUserAuthenticated() {
        return getAuthToken() != null;
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

        String token = getAuthToken();
        if (token == null) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    sendDriverLocationPing(token, location);
                } else {
                    requestCurrentLocationAndPing(token);
                }
            })
            .addOnFailureListener(error ->
                Log.w(TAG, "Failed to read last known location for driver ping.", error)
            );
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocationAndPing(String token) {
        if (fusedLocationClient == null) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    sendDriverLocationPing(token, location);
                }
            })
            .addOnFailureListener(error ->
                Log.w(TAG, "Failed to fetch current location for driver ping.", error)
            );
    }

    private void sendDriverLocationPing(String token, Location location) {
        if (driverApiService == null) {
            return;
        }

        DriverLocationUpdateRequest request =
            new DriverLocationUpdateRequest(location.getLatitude(), location.getLongitude());

        driverApiService.updateMyLocation("Bearer " + token, request).enqueue(new Callback<Void>() {
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

    private void clearAuthState() {
        getAuthPrefs().edit()
            .remove("token")
            .remove("userId")
            .remove("email")
            .remove("role")
            .apply();
    }

    private void checkActiveRides() {
        if (!isUserAuthenticated()) {
            return;
        }
        if (navController == null) {
            return;
        }
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == R.id.rideTrackingFragment) {
            return;
        }

        if (RideTrackingConfig.USE_MOCK_UPDATES) {
            navigateToRide(RideTrackingConfig.MOCK_RIDE_ID);
            return;
        }

        if (rideApiService == null) {
            return;
        }
        rideApiService.getActiveRides().enqueue(new Callback<List<RideResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<List<RideResponse>> call,
                @NonNull Response<List<RideResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    return;
                }
                RideResponse firstRide = response.body().get(0);
                if (firstRide == null || firstRide.id == null) {
                    return;
                }
                navigateToRide(firstRide.id);
            }

            @Override
            public void onFailure(@NonNull Call<List<RideResponse>> call, @NonNull Throwable t) {
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
