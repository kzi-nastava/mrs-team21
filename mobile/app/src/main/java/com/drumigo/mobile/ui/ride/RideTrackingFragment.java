package com.drumigo.mobile.ui.ride;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.DriverApiService;
import com.drumigo.mobile.data.api.MapboxApiClient;
import com.drumigo.mobile.data.api.MapboxDirectionsService;
import com.drumigo.mobile.data.api.PassengerApiService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.history.PassengerRideHistoryItemResponse;
import com.drumigo.mobile.data.mock.RideTrackingMockProvider;
import com.drumigo.mobile.data.model.mapbox.MapboxDirectionsResponse;
import com.drumigo.mobile.data.model.ride.ActiveRideIdResponse;
import com.drumigo.mobile.data.model.ride.ActiveRide;
import com.drumigo.mobile.data.model.ride.LocationUpdate;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;
import com.drumigo.mobile.data.model.ride.RoutePoint;
import com.drumigo.mobile.session.SessionManager;
import com.drumigo.mobile.databinding.FragmentRideTrackingBinding;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationsUtils;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions;
import com.mapbox.maps.plugin.attribution.AttributionUtils;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.plugin.logo.LogoUtils;
import com.mapbox.maps.plugin.scalebar.ScaleBarUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideTrackingFragment extends Fragment {

    private static final String ARG_RIDE_ID = "rideId";
    private static final String ROLE_DRIVER = "DRIVER";
    private static final String ROLE_PASSENGER = "PASSENGER";
    private static final double DEFAULT_LNG = 19.8200;
    private static final double DEFAULT_LAT = 45.2500;
    private static final double DEFAULT_ZOOM = 13.5;
    private static final String MAPBOX_STYLE_URI = "mapbox://styles/mapbox/streets-v12";
    private static final long LOCATION_POLLING_INTERVAL_MS = 2500L;
    private static final long ROUTE_REFRESH_INTERVAL_MS = 20_000L;
    private static final double WAYPOINT_REACHED_RADIUS_METERS = 70.0;
    private static final int MAPBOX_MAX_ROUTE_POINTS = 25;
    private static final double COORD_TOLERANCE = 1e-8;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final long PANEL_SHOW_ANIMATION_MS = 240L;
    private static final long PANEL_HIDE_ANIMATION_MS = 200L;

    private FragmentRideTrackingBinding binding;
    private MapView mapView;
    private boolean mapReady = false;
    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private Bitmap carIcon;
    private Bitmap startIcon;
    private Bitmap destinationIcon;

    private RideApiService rideApiService;
    private DriverApiService driverApiService;
    private PassengerApiService passengerApiService;
    private MapboxDirectionsService directionsService;
    private RideTrackingMockProvider mockProvider;
    private SessionManager sessionManager;

    private ActiveRide activeRide;
    private List<RoutePoint> renderedRoute = new ArrayList<>();
    private String currentRoleNormalized = "";
    private long rideId;
    private boolean pendingActiveRideLookup = false;
    private RoutePoint previousLocation;
    private int lastPassedWaypointOrder = Integer.MIN_VALUE;
    private float currentBearing = 0f;
    private boolean trackingPanelOpen = false;
    private boolean panicTriggeredForRide = false;
    private float panelDragStartY = 0f;
    private long lastRouteRequestAt = 0L;
    private int routeRequestToken = 0;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable backendPollingRunnable = new Runnable() {
        @Override
        public void run() {
            fetchRideTracking();
            pollingHandler.postDelayed(this, LOCATION_POLLING_INTERVAL_MS);
        }
    };

    public RideTrackingFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxOptions.setAccessToken(BuildConfig.MAPBOX_ACCESS_TOKEN);
        rideApiService = ApiClient.getRideApiService();
        driverApiService = ApiClient.getDriverApiService();
        passengerApiService = ApiClient.getPassengerApiService();
        directionsService = MapboxApiClient.getDirectionsService();
        mockProvider = new RideTrackingMockProvider();
        sessionManager = SessionManager.getInstance(requireContext());
        currentRoleNormalized = getCurrentRoleNormalized();
        rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, 0L) : 0L;
        if (rideId == 0L) {
            if (RideTrackingConfig.USE_MOCK_UPDATES) {
                rideId = RideTrackingConfig.MOCK_RIDE_ID;
            } else {
                pendingActiveRideLookup = true;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentRideTrackingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = binding.mapView;
        setupMap();
        setupActions();
        setupTrackingPanel();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
        startTracking();
    }

    @Override
    public void onStop() {
        stopTracking();
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onDestroyView() {
        stopTracking();
        if (mapView != null) {
            mapView.onDestroy();
        }
        mapView = null;
        pointAnnotationManager = null;
        polylineAnnotationManager = null;
        carIcon = null;
        startIcon = null;
        destinationIcon = null;
        binding = null;
        super.onDestroyView();
    }

    private void setupMap() {
        if (isMapboxTokenMissing()) {
            showMapError();
            return;
        }
        hideDefaultMapboxOrnaments();
        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
            .center(Point.fromLngLat(DEFAULT_LNG, DEFAULT_LAT))
            .zoom(DEFAULT_ZOOM)
            .build());
        mapView.getMapboxMap().loadStyleUri(MAPBOX_STYLE_URI, style -> {
            mapReady = true;
            updateMapContent();
        });
    }

    private void setupActions() {
        View.OnClickListener notImplementedListener = v -> showNotImplemented();
        binding.btnCancelRide.setOnClickListener(v -> openCancelRideSheet());
        binding.btnStopRide.setOnClickListener(v -> openStopRideSheet());
        binding.btnPanic.setOnClickListener(v -> openPanicSheet());
        binding.btnReportIssue.setOnClickListener(notImplementedListener);
        applyActionVisibility();
    }

    private void setupTrackingPanel() {
        binding.btnTrackingLauncher.setOnClickListener(v -> openTrackingPanel());
        binding.btnCloseTrackingPanel.setOnClickListener(v -> closeTrackingPanel());
        setupPanelSwipeToClose();
        initializeTrackingPanel();
    }

    private void setupPanelSwipeToClose() {
        View.OnTouchListener dragListener = (v, event) -> {
            if (binding == null || !trackingPanelOpen) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    panelDragStartY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - panelDragStartY;
                    if (deltaY > 0f) {
                        binding.trackingPanel.setTranslationY(deltaY);
                        float alpha = 1f - Math.min(0.12f, deltaY / 2200f);
                        binding.trackingPanel.setAlpha(alpha);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float draggedDistance = binding.trackingPanel.getTranslationY();
                    if (draggedDistance > 140f) {
                        closeTrackingPanel();
                    } else {
                        binding.trackingPanel.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(160L)
                            .start();
                    }
                    return true;
                default:
                    return false;
            }
        };
        binding.trackingDragHandle.setOnTouchListener(dragListener);
        View scrollView = binding.getRoot().findViewById(R.id.trackingScroll);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            });
        }
    }

    private void initializeTrackingPanel() {
        if (binding == null) {
            return;
        }
        trackingPanelOpen = false;
        adjustPanelHeightForScreen();
        binding.trackingPanel.setVisibility(View.GONE);
        binding.btnTrackingLauncher.setVisibility(View.VISIBLE);
        binding.trackingPanel.setTranslationY(0f);
        binding.trackingPanel.setAlpha(1f);
    }

    private void openPanicSheet() {
        if (binding == null || getContext() == null || rideApiService == null) {
            return;
        }
        if (pendingActiveRideLookup || rideId == 0L) {
            resolveActiveRideAndStartTracking();
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_loading_active_ride, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (panicTriggeredForRide) {
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_panic_already_sent, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String status = getRideStatusLabel();
        if (isRideStatusLoaded() && !isRideActiveStatus(status)) {
            Snackbar.make(
                binding.getRoot(),
                getString(R.string.ride_tracking_panic_unavailable, status),
                Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        RidePanicBottomSheet.show(
            requireContext(),
            rideApiService,
            rideId,
            activeRide,
            panicTriggeredForRide,
            new RidePanicBottomSheet.CallbackHandler() {
                @Override
                public void onPanicSent() {
                    panicTriggeredForRide = true;
                    if (binding != null) {
                        Snackbar.make(
                            binding.getRoot(),
                            R.string.ride_tracking_panic_sent_snackbar,
                            Snackbar.LENGTH_SHORT
                        ).show();
                    }
                }
            }
        );
    }

    private void openStopRideSheet() {
        if (binding == null || getContext() == null || rideApiService == null) {
            return;
        }
        if (!isDriverRole()) {
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_stop_driver_only, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (pendingActiveRideLookup || rideId == 0L) {
            resolveActiveRideAndStartTracking();
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_loading_active_ride, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (activeRide == null) {
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_load_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String status = getRideStatusLabel();
        if (!isRideActiveStatus(status)) {
            Snackbar.make(
                binding.getRoot(),
                getString(R.string.ride_tracking_stop_active_only, status),
                Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        RideStopBottomSheet.show(
            requireContext(),
            rideApiService,
            rideId,
            activeRide,
            response -> {
                ActiveRide updated = mapRideResponseToActiveRide(response, activeRide);
                applyRide(updated);
                requestRouteIfNeeded(true);
                Snackbar.make(binding.getRoot(), R.string.ride_tracking_stop_success, Snackbar.LENGTH_SHORT).show();
            }
        );
    }

    private void openCancelRideSheet() {
        if (binding == null || getContext() == null || rideApiService == null) {
            return;
        }
        if (!isDriverRole() && !isPassengerRole()) {
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_cancel_unavailable_role, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (pendingActiveRideLookup || rideId == 0L) {
            resolveActiveRideAndStartTracking();
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_loading_active_ride, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (activeRide == null) {
            Snackbar.make(binding.getRoot(), R.string.ride_tracking_load_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        RideCancelBottomSheet.show(
            requireContext(),
            rideApiService,
            rideId,
            activeRide,
            isDriverRole(),
            () -> {
                if (activeRide != null) {
                    activeRide.status = "CANCELLED";
                    applyRide(activeRide);
                }
                Snackbar.make(binding.getRoot(), R.string.ride_tracking_cancel_success, Snackbar.LENGTH_SHORT).show();
            }
        );
    }

    private boolean isRideStatusLoaded() {
        return activeRide != null
            && activeRide.status != null
            && !activeRide.status.trim().isEmpty();
    }

    private String getRideStatusLabel() {
        if (!isRideStatusLoaded()) {
            return getString(R.string.ride_tracking_panic_status_unknown);
        }
        return activeRide.status.trim();
    }

    private boolean isRideActiveStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = normalizeRideStatus(status);
        return "ACTIVE".equals(normalized) || "IN_PROGRESS".equals(normalized);
    }

    private boolean isRideCancelableForDriver(String status) {
        String normalized = normalizeRideStatus(status);
        return "PENDING".equals(normalized)
            || "ACCEPTED".equals(normalized)
            || "DRIVER_ARRIVING".equals(normalized);
    }

    private boolean isRideCancelableForPassenger(String status) {
        String normalized = normalizeRideStatus(status);
        return "PENDING".equals(normalized)
            || "ACCEPTED".equals(normalized);
    }

    private boolean isRideTerminalStatus(String status) {
        String normalized = normalizeRideStatus(status);
        return "FINISHED".equals(normalized)
            || "COMPLETED".equals(normalized)
            || "CANCELLED".equals(normalized)
            || "REJECTED".equals(normalized);
    }

    private boolean isTrackableRideStatus(String status) {
        String normalized = normalizeRideStatus(status);
        return "PENDING".equals(normalized)
            || "ACCEPTED".equals(normalized)
            || "DRIVER_ARRIVING".equals(normalized)
            || "ACTIVE".equals(normalized)
            || "IN_PROGRESS".equals(normalized);
    }

    private String normalizeRideStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toUpperCase()
            .replace('-', '_')
            .replace(' ', '_');
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

    private boolean isDriverRole() {
        return ROLE_DRIVER.equals(currentRoleNormalized);
    }

    private boolean isPassengerRole() {
        return ROLE_PASSENGER.equals(currentRoleNormalized);
    }

    private void applyActionVisibility() {
        if (binding == null) {
            return;
        }
        boolean showDriverControls = isDriverRole();
        boolean showPassengerControls = isPassengerRole();
        boolean rideActive = isRideActiveStatus(getRideStatusLabel());

        binding.btnCancelRide.setVisibility((showDriverControls || showPassengerControls) ? View.VISIBLE : View.GONE);
        binding.btnStopRide.setVisibility(showDriverControls ? View.VISIBLE : View.GONE);
        binding.btnReportIssue.setVisibility(showPassengerControls ? View.VISIBLE : View.GONE);
        binding.btnPanic.setVisibility((showDriverControls || showPassengerControls) ? View.VISIBLE : View.GONE);

        binding.btnCancelRide.setEnabled(showDriverControls || showPassengerControls);
        if (showDriverControls) {
            binding.btnStopRide.setEnabled(rideActive);
        }
        if (showPassengerControls) {
            binding.btnReportIssue.setEnabled(rideActive);
        }
        binding.btnPanic.setEnabled(rideActive || !isRideStatusLoaded());
    }

    private void openTrackingPanel() {
        if (binding == null || trackingPanelOpen) {
            return;
        }
        trackingPanelOpen = true;
        adjustPanelHeightForScreen();
        binding.btnTrackingLauncher.setVisibility(View.GONE);
        binding.trackingPanel.setVisibility(View.VISIBLE);
        binding.trackingPanel.post(() -> {
            if (binding == null) {
                return;
            }
            float startOffset = binding.getRoot().getHeight();
            binding.trackingPanel.setTranslationY(startOffset);
            binding.trackingPanel.setAlpha(0.96f);
            binding.trackingPanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(PANEL_SHOW_ANIMATION_MS)
                .start();
        });
    }

    private void closeTrackingPanel() {
        if (binding == null || !trackingPanelOpen) {
            return;
        }
        trackingPanelOpen = false;
        float endOffset = binding.getRoot().getHeight();
        binding.trackingPanel.animate()
            .translationY(endOffset)
            .alpha(0.96f)
            .setDuration(PANEL_HIDE_ANIMATION_MS)
            .withEndAction(() -> {
                if (binding == null) {
                    return;
                }
                binding.trackingPanel.setVisibility(View.GONE);
                binding.trackingPanel.setTranslationY(0f);
                binding.trackingPanel.setAlpha(1f);
                binding.btnTrackingLauncher.setVisibility(View.VISIBLE);
            })
            .start();
    }

    private void adjustPanelHeightForScreen() {
        if (binding == null) {
            return;
        }
        binding.getRoot().post(() -> {
            if (binding == null) {
                return;
            }
            int rootHeight = binding.getRoot().getHeight();
            if (rootHeight <= 0) {
                return;
            }
            int maxPanelHeight = (int) (rootHeight * 0.78f);
            ViewGroup.LayoutParams panelParams = binding.trackingPanel.getLayoutParams();
            panelParams.height = maxPanelHeight;
            binding.trackingPanel.setLayoutParams(panelParams);
        });
    }

    private void hideDefaultMapboxOrnaments() {
        if (mapView == null) {
            return;
        }
        try {
            LogoUtils.getLogo(mapView).setEnabled(false);
        } catch (Exception ignored) {
        }
        try {
            ScaleBarUtils.getScaleBar(mapView).setEnabled(false);
        } catch (Exception ignored) {
        }
        try {
            AttributionUtils.getAttribution(mapView).setEnabled(false);
        } catch (Exception ignored) {
        }
    }

    private boolean isMapboxTokenMissing() {
        String token = BuildConfig.MAPBOX_ACCESS_TOKEN;
        return token == null || token.trim().isEmpty() || "MAPBOX_API_KEY".equals(token);
    }

    private void showMapError() {
        if (binding == null) {
            return;
        }
        binding.mapError.setVisibility(View.VISIBLE);
        binding.mapError.setText(R.string.mapbox_token_missing);
    }

    private void startTracking() {
        if (pendingActiveRideLookup || rideId == 0L) {
            resolveActiveRideAndStartTracking();
            return;
        }
        if (RideTrackingConfig.USE_MOCK_UPDATES) {
            ActiveRide ride = mockProvider.getActiveRide(rideId);
            applyRide(ride);
            mockProvider.startLocationUpdates(rideId, this::applyLocationUpdate);
            requestRouteIfNeeded(true);
        } else {
            fetchRideTracking();
            pollingHandler.removeCallbacks(backendPollingRunnable);
            pollingHandler.postDelayed(backendPollingRunnable, LOCATION_POLLING_INTERVAL_MS);
        }
    }

    private void stopTracking() {
        pollingHandler.removeCallbacks(backendPollingRunnable);
        mockProvider.stopLocationUpdates();
    }

    private void resolveActiveRideAndStartTracking() {
        if (rideApiService == null) {
            showRideLoadError();
            return;
        }
        rideApiService.getMyActiveRide().enqueue(new Callback<ActiveRideIdResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<ActiveRideIdResponse> call,
                @NonNull Response<ActiveRideIdResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().rideId != null) {
                    startBackendTrackingForRide(response.body().rideId);
                    return;
                }
                if (response.code() == 404) {
                    resolveFallbackTrackableRide();
                } else {
                    showNoActiveRideError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ActiveRideIdResponse> call, @NonNull Throwable t) {
                showRideLoadError();
            }
        });
    }

    private void resolveFallbackTrackableRide() {
        if (sessionManager == null) {
            showNoActiveRideError();
            return;
        }
        long userId = sessionManager.getUserId();
        if (userId <= 0L) {
            showNoActiveRideError();
            return;
        }
        if (isPassengerRole()) {
            resolvePassengerFallbackRide(userId);
            return;
        }
        if (isDriverRole()) {
            resolveDriverFallbackRide(userId);
            return;
        }
        showNoActiveRideError();
    }

    private void resolvePassengerFallbackRide(long userId) {
        if (passengerApiService == null) {
            showNoActiveRideError();
            return;
        }
        passengerApiService.getPassengerRideHistory(
            userId,
            null,
            null,
            0,
            30,
            "requestedAt,desc"
        ).enqueue(new Callback<PageResponse<PassengerRideHistoryItemResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<PageResponse<PassengerRideHistoryItemResponse>> call,
                @NonNull Response<PageResponse<PassengerRideHistoryItemResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                    showNoActiveRideError();
                    return;
                }
                Long fallbackRideId = null;
                for (PassengerRideHistoryItemResponse item : response.body().content) {
                    if (item == null || item.id == null) {
                        continue;
                    }
                    if (isTrackableRideStatus(item.status)) {
                        fallbackRideId = item.id;
                        break;
                    }
                }
                if (fallbackRideId == null) {
                    showNoActiveRideError();
                    return;
                }
                startBackendTrackingForRide(fallbackRideId);
            }

            @Override
            public void onFailure(
                @NonNull Call<PageResponse<PassengerRideHistoryItemResponse>> call,
                @NonNull Throwable t
            ) {
                showNoActiveRideError();
            }
        });
    }

    private void resolveDriverFallbackRide(long userId) {
        if (driverApiService == null) {
            showNoActiveRideError();
            return;
        }
        driverApiService.getUpcomingDriverRides(
            userId,
            null,
            0,
            20,
            "scheduledFor,asc"
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<PageResponse<RideResponse>> call,
                @NonNull Response<PageResponse<RideResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                    showNoActiveRideError();
                    return;
                }
                Long fallbackRideId = null;
                for (RideResponse item : response.body().content) {
                    if (item == null || item.id == null) {
                        continue;
                    }
                    if (isTrackableRideStatus(item.status)) {
                        fallbackRideId = item.id;
                        break;
                    }
                }
                if (fallbackRideId == null) {
                    showNoActiveRideError();
                    return;
                }
                startBackendTrackingForRide(fallbackRideId);
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<RideResponse>> call, @NonNull Throwable t) {
                showNoActiveRideError();
            }
        });
    }

    private void startBackendTrackingForRide(long resolvedRideId) {
        rideId = resolvedRideId;
        pendingActiveRideLookup = false;
        fetchRideTracking();
        pollingHandler.removeCallbacks(backendPollingRunnable);
        pollingHandler.postDelayed(backendPollingRunnable, LOCATION_POLLING_INTERVAL_MS);
    }

    private void fetchRideTracking() {
        if (rideApiService == null) {
            return;
        }
        rideApiService.getRideTracking(rideId).enqueue(new Callback<RideTrackingResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideTrackingResponse> call,
                @NonNull Response<RideTrackingResponse> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    showRideLoadError();
                    return;
                }
                ActiveRide ride = mapToActiveRide(response.body());
                if (ride == null) {
                    showRideLoadError();
                    return;
                }
                applyRide(ride);
                updateFromTrackingResponse(response.body());
                updateCompletionState();
                requestRouteIfNeeded(false);
            }

            @Override
            public void onFailure(@NonNull Call<RideTrackingResponse> call, @NonNull Throwable t) {
                showRideLoadError();
            }
        });
    }

    private void updateFromTrackingResponse(RideTrackingResponse response) {
        if (response == null || activeRide == null) {
            return;
        }
        Double lat = response.vehicleCurrentLat;
        Double lng = response.vehicleCurrentLng;
        if (lat == null || lng == null) {
            return;
        }
        RoutePoint current = new RoutePoint(lat, lng, 0);
        int eta = response.estimatedDurationSec != null ? response.estimatedDurationSec : 0;
        float bearing = 0f;
        if (previousLocation != null) {
            bearing = (float) calculateBearing(previousLocation, current);
        }
        previousLocation = current;
        applyLocationUpdate(new LocationUpdate(lat, lng, System.currentTimeMillis(), eta, bearing));
    }

    private void applyRide(ActiveRide ride) {
        boolean destinationChanged = didDestinationChange(activeRide, ride);
        activeRide = ride;
        if (destinationChanged) {
            renderedRoute.clear();
            lastPassedWaypointOrder = Integer.MIN_VALUE;
            routeRequestToken++;
        }
        if (binding == null || ride == null) {
            return;
        }
        String rideIdLabel = ride.id == null ? "--" : String.valueOf(ride.id);
        binding.rideId.setText(getString(R.string.ride_tracking_id_format, rideIdLabel));
        binding.driverName.setText(buildDriverName(ride.driverName, ride.driverSurname));
        binding.vehicleInfo.setText(buildVehicleInfo(ride.vehicleModel, ride.vehicleLicensePlate));
        binding.fromAddress.setText(ride.startAddress == null ? "" : ride.startAddress);
        binding.toAddress.setText(ride.destinationAddress == null ? "" : ride.destinationAddress);
        binding.etaValue.setText(formatEta(ride.estimatedArrivalTimeSec));
        applyActionVisibility();
        updateMapContent();
        updateCompletionState();
    }

    private void applyLocationUpdate(LocationUpdate update) {
        if (activeRide == null || update == null) {
            return;
        }
        activeRide.currentLocation = new RoutePoint(update.lat, update.lng, 0);
        activeRide.estimatedArrivalTimeSec = update.estimatedArrivalTimeSec;
        currentBearing = update.bearing;
        if (binding != null) {
            binding.etaValue.setText(formatEta(update.estimatedArrivalTimeSec));
        }
        updateMapContent();
        requestRouteIfNeeded(false);
    }

    private void updateCompletionState() {
        if (binding == null || activeRide == null) {
            return;
        }
        boolean isCompleted = isRideTerminalStatus(activeRide.status);
        binding.rideCompletedMessage.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
        if (isCompleted) {
            stopTracking();
        }
    }

    private void updateMapContent() {
        if (!mapReady || activeRide == null) {
            return;
        }
        ensureAnnotationManagers();
        updateRouteLine();
        updateMarkers();
        updateMapCamera();
    }

    private void updateMarkers() {
        if (pointAnnotationManager == null || activeRide == null) {
            return;
        }
        pointAnnotationManager.deleteAll();

        if (activeRide.startLocation != null) {
            pointAnnotationManager.create(new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(activeRide.startLocation.lng, activeRide.startLocation.lat))
                .withIconImage(getStartIcon())
                .withIconAnchor(IconAnchor.CENTER));
        }

        if (activeRide.destinationLocation != null) {
            pointAnnotationManager.create(new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(activeRide.destinationLocation.lng, activeRide.destinationLocation.lat))
                .withIconImage(getDestinationIcon())
                .withIconAnchor(IconAnchor.CENTER));
        }

        if (activeRide.currentLocation != null) {
            pointAnnotationManager.create(new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(activeRide.currentLocation.lng, activeRide.currentLocation.lat))
                .withIconImage(getCarIcon())
                .withIconRotate(currentBearing)
                .withIconAnchor(IconAnchor.CENTER));
        }
    }

    private void updateRouteLine() {
        if (polylineAnnotationManager == null || activeRide == null) {
            return;
        }
        List<RoutePoint> source = null;
        if (renderedRoute != null && renderedRoute.size() >= 2) {
            source = renderedRoute;
        } else if (activeRide.route != null && activeRide.route.size() > 2) {
            // Use backend route only when it looks like a true path, not a straight 2-point segment.
            source = activeRide.route;
        }
        polylineAnnotationManager.deleteAll();
        if (source == null || source.size() < 2) {
            return;
        }
        List<Point> points = new ArrayList<>();
        for (RoutePoint routePoint : source) {
            points.add(Point.fromLngLat(routePoint.lng, routePoint.lat));
        }
        if (!points.isEmpty()) {
            polylineAnnotationManager.create(new PolylineAnnotationOptions()
                .withPoints(points)
                .withLineColor("#5B4CDB")
                .withLineWidth(5.0));
        }
    }

    private void updateMapCamera() {
        if (activeRide == null || activeRide.currentLocation == null || mapView == null) {
            return;
        }
        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
            .center(Point.fromLngLat(activeRide.currentLocation.lng, activeRide.currentLocation.lat))
            .zoom(DEFAULT_ZOOM + 2.0)
            .build());
    }

    private void requestRouteIfNeeded(boolean force) {
        if (activeRide == null || activeRide.currentLocation == null || activeRide.destinationLocation == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastRouteRequestAt < ROUTE_REFRESH_INTERVAL_MS) {
            return;
        }
        lastRouteRequestAt = now;
        List<RoutePoint> requestPoints = deduplicateConsecutivePoints(
            buildFallbackCurrentToDestination(activeRide.currentLocation)
        );
        if (requestPoints.size() < 2) {
            return;
        }
        int requestToken = ++routeRequestToken;
        fetchRouteFromMapbox(requestPoints, "driving-traffic", true, requestToken);
    }

    private List<RoutePoint> buildRouteRequestPoints(RoutePoint currentLocation) {
        if (activeRide == null || activeRide.route == null || activeRide.route.isEmpty()) {
            return deduplicateConsecutivePoints(buildFallbackCurrentToDestination(currentLocation));
        }

        List<RoutePoint> sortedWaypoints = new ArrayList<>(activeRide.route);
        Collections.sort(sortedWaypoints, Comparator.comparingInt(wp -> wp.order));
        if (sortedWaypoints.size() < 2) {
            return deduplicateConsecutivePoints(buildFallbackCurrentToDestination(currentLocation));
        }

        RoutePoint destination = sortedWaypoints.get(sortedWaypoints.size() - 1);
        int reachedOrder = Integer.MIN_VALUE;
        for (int i = 0; i < sortedWaypoints.size() - 1; i++) {
            RoutePoint waypoint = sortedWaypoints.get(i);
            double distance = distanceMeters(currentLocation, waypoint);
            if (distance <= WAYPOINT_REACHED_RADIUS_METERS) {
                reachedOrder = Math.max(reachedOrder, waypoint.order);
            }
        }
        lastPassedWaypointOrder = Math.max(lastPassedWaypointOrder, reachedOrder);

        RoutePoint origin = lastPassedWaypointOrder == Integer.MIN_VALUE
            ? sortedWaypoints.get(0)
            : currentLocation;

        List<RoutePoint> points = new ArrayList<>();
        points.add(copyPoint(origin));
        for (RoutePoint waypoint : sortedWaypoints) {
            if (waypoint.order > lastPassedWaypointOrder) {
                points.add(copyPoint(waypoint));
            }
        }
        points = deduplicateConsecutivePoints(points);
        if (points.size() < 2) {
            points.add(copyPoint(destination));
        }
        if (points.size() > MAPBOX_MAX_ROUTE_POINTS) {
            points = new ArrayList<>(points.subList(0, MAPBOX_MAX_ROUTE_POINTS));
            RoutePoint last = points.get(points.size() - 1);
            if (!isSamePoint(last, destination)) {
                points.set(points.size() - 1, copyPoint(destination));
            }
        }
        return deduplicateConsecutivePoints(points);
    }

    private List<RoutePoint> buildFallbackCurrentToDestination(RoutePoint currentLocation) {
        List<RoutePoint> points = new ArrayList<>();
        points.add(copyPoint(currentLocation));
        if (activeRide != null && activeRide.destinationLocation != null) {
            points.add(copyPoint(activeRide.destinationLocation));
        }
        return points;
    }

    private List<RoutePoint> deduplicateConsecutivePoints(List<RoutePoint> points) {
        if (points == null || points.size() < 2) {
            return points == null ? new ArrayList<>() : points;
        }
        List<RoutePoint> result = new ArrayList<>();
        result.add(points.get(0));
        for (int i = 1; i < points.size(); i++) {
            RoutePoint previous = result.get(result.size() - 1);
            RoutePoint current = points.get(i);
            if (!isSamePoint(previous, current)) {
                result.add(current);
            }
        }
        return result;
    }

    private boolean isSamePoint(RoutePoint first, RoutePoint second) {
        if (first == null || second == null) {
            return false;
        }
        return Math.abs(first.lat - second.lat) <= COORD_TOLERANCE
            && Math.abs(first.lng - second.lng) <= COORD_TOLERANCE;
    }

    private RoutePoint copyPoint(RoutePoint source) {
        if (source == null) {
            return null;
        }
        return new RoutePoint(source.lat, source.lng, source.order);
    }

    private double distanceMeters(RoutePoint first, RoutePoint second) {
        if (first == null || second == null) {
            return 0.0;
        }
        double lat1 = Math.toRadians(first.lat);
        double lat2 = Math.toRadians(second.lat);
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(second.lng - first.lng);
        double sinLat = Math.sin(dLat / 2.0);
        double sinLng = Math.sin(dLng / 2.0);
        double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private void fetchRouteFromMapbox(
        List<RoutePoint> routeRequestPoints,
        String profile,
        boolean allowFallback,
        int requestToken
    ) {
        if (directionsService == null) {
            fallbackToRoutePoints();
            return;
        }
        String coordinates = buildCoordinatesString(routeRequestPoints);
        if (coordinates.isEmpty()) {
            fallbackToRoutePoints();
            return;
        }
        directionsService.getDirections(
            profile,
            coordinates,
            "geojson",
            "full",
            BuildConfig.MAPBOX_ACCESS_TOKEN
        ).enqueue(new Callback<MapboxDirectionsResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<MapboxDirectionsResponse> call,
                @NonNull Response<MapboxDirectionsResponse> response
            ) {
                if (requestToken != routeRequestToken) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && hasRouteCoordinates(response.body())) {
                    updateRouteFromMapbox(response.body());
                } else if (allowFallback) {
                    fetchRouteFromMapbox(routeRequestPoints, "driving", false, requestToken);
                } else {
                    fallbackToRoutePoints();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxDirectionsResponse> call, @NonNull Throwable t) {
                if (requestToken != routeRequestToken) {
                    return;
                }
                if (allowFallback) {
                    fetchRouteFromMapbox(routeRequestPoints, "driving", false, requestToken);
                } else {
                    fallbackToRoutePoints();
                }
            }
        });
    }

    private String buildCoordinatesString(List<RoutePoint> points) {
        if (points == null || points.size() < 2) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            RoutePoint point = points.get(i);
            if (point == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(";");
            }
            builder.append(point.lng).append(",").append(point.lat);
        }
        return builder.toString();
    }

    private boolean hasRouteCoordinates(MapboxDirectionsResponse response) {
        return response.routes != null
            && !response.routes.isEmpty()
            && response.routes.get(0).geometry != null
            && response.routes.get(0).geometry.coordinates != null
            && !response.routes.get(0).geometry.coordinates.isEmpty();
    }

    private void updateRouteFromMapbox(MapboxDirectionsResponse response) {
        if (activeRide == null || response.routes == null || response.routes.isEmpty()) {
            return;
        }
        List<List<Double>> coordinates = response.routes.get(0).geometry.coordinates;
        List<RoutePoint> route = new ArrayList<>();
        int order = 0;
        for (List<Double> pair : coordinates) {
            if (pair.size() < 2) {
                continue;
            }
            double lng = pair.get(0);
            double lat = pair.get(1);
            route.add(new RoutePoint(lat, lng, order++));
        }
        if (!route.isEmpty()) {
            renderedRoute = route;
            updateMapContent();
        }
    }

    private void fallbackToRoutePoints() {
        if (activeRide == null || activeRide.route == null || activeRide.route.size() <= 2) {
            return;
        }
        if (renderedRoute == null || renderedRoute.isEmpty()) {
            renderedRoute = new ArrayList<>(activeRide.route);
        }
        updateMapContent();
    }

    private boolean didDestinationChange(ActiveRide previous, ActiveRide current) {
        if (previous == null || current == null) {
            return false;
        }
        if (previous.destinationLocation == null || current.destinationLocation == null) {
            return false;
        }
        return !isSamePoint(previous.destinationLocation, current.destinationLocation);
    }

    private void ensureAnnotationManagers() {
        if (mapView == null) {
            return;
        }
        AnnotationPlugin annotationPlugin = AnnotationsUtils.getAnnotations(mapView);
        if (pointAnnotationManager == null) {
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, null);
        }
        if (polylineAnnotationManager == null) {
            polylineAnnotationManager = PolylineAnnotationManagerKt.createPolylineAnnotationManager(annotationPlugin, null);
        }
    }

    private Bitmap getCarIcon() {
        if (carIcon == null) {
            carIcon = createTintedMarker(R.drawable.ic_car, R.color.primary);
        }
        return carIcon;
    }

    private Bitmap getStartIcon() {
        if (startIcon == null) {
            startIcon = createTintedMarker(R.drawable.ic_circle, R.color.primary);
        }
        return startIcon;
    }

    private Bitmap getDestinationIcon() {
        if (destinationIcon == null) {
            destinationIcon = createTintedMarker(R.drawable.ic_circle, R.color.success);
        }
        return destinationIcon;
    }

    private Bitmap createTintedMarker(int drawableResId, int colorResId) {
        if (getContext() == null) {
            return null;
        }
        Drawable drawable = ContextCompat.getDrawable(requireContext(), drawableResId);
        if (drawable == null) {
            return null;
        }
        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(requireContext(), colorResId));
        int width = wrapped.getIntrinsicWidth() > 0 ? wrapped.getIntrinsicWidth() : 48;
        int height = wrapped.getIntrinsicHeight() > 0 ? wrapped.getIntrinsicHeight() : 48;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        wrapped.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        wrapped.draw(canvas);
        return bitmap;
    }

    private ActiveRide mapToActiveRide(RideTrackingResponse response) {
        if (response == null) {
            return null;
        }
        ActiveRide ride = new ActiveRide();
        ride.id = response.id;
        ride.status = response.status;
        ride.driverName = response.driverName;
        ride.driverSurname = response.driverSurname;
        ride.vehicleModel = response.vehicleModel;
        ride.vehicleLicensePlate = response.vehicleLicensePlate;
        ride.estimatedArrivalTimeSec = response.estimatedDurationSec != null
            ? response.estimatedDurationSec
            : 0;
        ride.totalDistanceKm = response.totalDistanceKm;

        List<RideTrackingResponse.WaypointInfo> waypoints = response.waypoints == null
            ? new ArrayList<>()
            : new ArrayList<>(response.waypoints);
        Collections.sort(waypoints, Comparator.comparingInt(wp -> wp.order == null ? 0 : wp.order));
        if (!waypoints.isEmpty()) {
            RideTrackingResponse.WaypointInfo first = waypoints.get(0);
            RideTrackingResponse.WaypointInfo last = waypoints.get(waypoints.size() - 1);
            ride.startAddress = first.address;
            ride.destinationAddress = last.address;
            if (first.lat != null && first.lng != null) {
                ride.startLocation = new RoutePoint(first.lat, first.lng, first.order == null ? 0 : first.order);
            }
            if (last.lat != null && last.lng != null) {
                ride.destinationLocation = new RoutePoint(last.lat, last.lng, last.order == null ? 0 : last.order);
            }
            List<RoutePoint> route = new ArrayList<>();
            for (RideTrackingResponse.WaypointInfo waypoint : waypoints) {
                if (waypoint.lat == null || waypoint.lng == null) {
                    continue;
                }
                int order = waypoint.order == null ? 0 : waypoint.order;
                route.add(new RoutePoint(waypoint.lat, waypoint.lng, order));
            }
            ride.route = route;
        }

        if (response.vehicleCurrentLat != null && response.vehicleCurrentLng != null) {
            ride.currentLocation = new RoutePoint(response.vehicleCurrentLat, response.vehicleCurrentLng, 0);
        } else {
            ride.currentLocation = ride.startLocation;
        }
        return ride;
    }

    private ActiveRide mapRideResponseToActiveRide(RideResponse response, ActiveRide previousRide) {
        ActiveRide ride = new ActiveRide();
        ride.id = response.id;
        ride.status = response.status;
        ride.driverName = response.driverName != null ? response.driverName : previousRide.driverName;
        ride.driverSurname = response.driverSurname != null ? response.driverSurname : previousRide.driverSurname;
        ride.vehicleModel = previousRide.vehicleModel;
        ride.vehicleLicensePlate = previousRide.vehicleLicensePlate;
        ride.estimatedArrivalTimeSec = response.estimatedDurationSec != null ? response.estimatedDurationSec : 0;
        ride.totalDistanceKm = response.totalDistanceKm != null ? response.totalDistanceKm : previousRide.totalDistanceKm;

        List<RideResponse.WaypointInfo> waypoints = response.waypoints == null
            ? new ArrayList<>()
            : new ArrayList<>(response.waypoints);
        Collections.sort(waypoints, Comparator.comparingInt(wp -> wp.order == null ? 0 : wp.order));

        List<RoutePoint> route = new ArrayList<>();
        for (RideResponse.WaypointInfo waypoint : waypoints) {
            if (waypoint == null || waypoint.lat == null || waypoint.lng == null) {
                continue;
            }
            int order = waypoint.order == null ? 0 : waypoint.order;
            route.add(new RoutePoint(waypoint.lat, waypoint.lng, order));
        }
        ride.route = route;

        if (!waypoints.isEmpty()) {
            RideResponse.WaypointInfo first = waypoints.get(0);
            RideResponse.WaypointInfo last = waypoints.get(waypoints.size() - 1);
            ride.startAddress = first.address;
            ride.destinationAddress = last.address;
            if (first.lat != null && first.lng != null) {
                ride.startLocation = new RoutePoint(first.lat, first.lng, first.order == null ? 0 : first.order);
            }
            if (last.lat != null && last.lng != null) {
                ride.destinationLocation = new RoutePoint(last.lat, last.lng, last.order == null ? 0 : last.order);
                ride.currentLocation = new RoutePoint(last.lat, last.lng, last.order == null ? 0 : last.order);
            }
        } else {
            ride.startAddress = previousRide.startAddress;
            ride.destinationAddress = previousRide.destinationAddress;
            ride.startLocation = previousRide.startLocation;
            ride.destinationLocation = previousRide.destinationLocation;
            ride.currentLocation = previousRide.currentLocation;
        }

        if (ride.currentLocation == null && previousRide.currentLocation != null) {
            ride.currentLocation = previousRide.currentLocation;
        }
        return ride;
    }

    private String buildDriverName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty() && last.isEmpty()) {
            return getString(R.string.ride_tracking_driver_placeholder);
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private String buildVehicleInfo(String model, String licensePlate) {
        String safeModel = model == null ? "" : model.trim();
        String safePlate = licensePlate == null ? "" : licensePlate.trim();
        if (safeModel.isEmpty() && safePlate.isEmpty()) {
            return getString(R.string.ride_tracking_vehicle_placeholder);
        }
        if (safeModel.isEmpty()) {
            return safePlate;
        }
        if (safePlate.isEmpty()) {
            return safeModel;
        }
        return safeModel + " - " + safePlate;
    }

    private String formatEta(Integer etaSeconds) {
        if (etaSeconds == null || etaSeconds <= 0) {
            return getString(R.string.ride_tracking_eta_placeholder);
        }
        int minutes = Math.max(1, etaSeconds / 60);
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return hours + "h";
        }
        return hours + "h " + remainingMinutes + "m";
    }

    private void showRideLoadError() {
        if (binding == null) {
            return;
        }
        Snackbar.make(
            binding.getRoot(),
            R.string.ride_tracking_load_failed,
            Snackbar.LENGTH_SHORT
        ).show();
    }

    private void showNoActiveRideError() {
        if (binding == null) {
            return;
        }
        Snackbar.make(
            binding.getRoot(),
            R.string.ride_tracking_no_active_ride,
            Snackbar.LENGTH_SHORT
        ).show();
    }

    private void showNotImplemented() {
        if (binding == null) {
            return;
        }
        Snackbar.make(
            binding.getRoot(),
            R.string.ride_tracking_not_implemented,
            Snackbar.LENGTH_SHORT
        ).show();
    }

    private double calculateBearing(RoutePoint from, RoutePoint to) {
        double dLng = Math.toRadians(to.lng - from.lng);
        double lat1 = Math.toRadians(from.lat);
        double lat2 = Math.toRadians(to.lat);
        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
            - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        double bearing = Math.atan2(y, x);
        bearing = Math.toDegrees(bearing);
        return (bearing + 360.0) % 360.0;
    }
}
