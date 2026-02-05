package com.drumigo.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.ui.ride.RideTrackingConfig;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private RideApiService rideApiService;
    private final Handler ridePollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable ridePollingRunnable = new Runnable() {
        @Override
        public void run() {
            checkActiveRides();
            ridePollingHandler.postDelayed(this, 10_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupEdgeToEdge();

        setupToolbar();

        setupNavigation();
        
        setupDrawer();

        setupBackPressedHandler();

        rideApiService = ApiClient.getRideApiService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRideAutoTracking();
    }

    @Override
    protected void onStop() {
        stopRideAutoTracking();
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
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                updateSelectedNavItem(destination.getId());
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
        if (destinationId == R.id.activeVehiclesMapFragment) {
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_active_vehicles) {
            navController.navigate(R.id.activeVehiclesMapFragment);
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
            navController.navigate(R.id.loginFragment);
        } else if (itemId == R.id.nav_ride_history) {
            // Launch RideHistoryActivity
            Intent intent = new Intent(this, com.ridesharing.app.ui.history.RideHistoryActivity.class);
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
        ridePollingHandler.removeCallbacks(ridePollingRunnable);
        ridePollingHandler.post(ridePollingRunnable);
    }

    private void stopRideAutoTracking() {
        ridePollingHandler.removeCallbacks(ridePollingRunnable);
    }

    private void checkActiveRides() {
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
