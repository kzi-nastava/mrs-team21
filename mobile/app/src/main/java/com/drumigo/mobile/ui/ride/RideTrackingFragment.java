package com.drumigo.mobile.ui.ride;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.MapboxApiClient;
import com.drumigo.mobile.data.api.MapboxDirectionsService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.mock.RideTrackingMockProvider;
import com.drumigo.mobile.data.model.mapbox.MapboxDirectionsResponse;
import com.drumigo.mobile.data.model.ride.ActiveRide;
import com.drumigo.mobile.data.model.ride.LocationUpdate;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;
import com.drumigo.mobile.data.model.ride.RoutePoint;
import com.drumigo.mobile.databinding.FragmentRideTrackingBinding;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideTrackingFragment extends Fragment {

    private static final String ARG_RIDE_ID = "rideId";
    private static final double DEFAULT_LNG = 19.8200;
    private static final double DEFAULT_LAT = 45.2500;
    private static final double DEFAULT_ZOOM = 13.5;
    private static final String MAPBOX_STYLE_URI = "mapbox://styles/mapbox/streets-v12";
    private static final long LOCATION_POLLING_INTERVAL_MS = 2500L;
    private static final long ROUTE_REFRESH_INTERVAL_MS = 20_000L;

    private FragmentRideTrackingBinding binding;
    private MapView mapView;
    private boolean mapReady = false;
    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private Bitmap carIcon;
    private Bitmap startIcon;
    private Bitmap destinationIcon;

    private RideApiService rideApiService;
    private MapboxDirectionsService directionsService;
    private RideTrackingMockProvider mockProvider;

    private ActiveRide activeRide;
    private long rideId;
    private RoutePoint previousLocation;
    private float currentBearing = 0f;
    private long lastRouteRequestAt = 0L;
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
        directionsService = MapboxApiClient.getDirectionsService();
        mockProvider = new RideTrackingMockProvider();
        rideId = getArguments() != null ? getArguments().getLong(ARG_RIDE_ID, 0L) : 0L;
        if (rideId == 0L) {
            rideId = RideTrackingConfig.MOCK_RIDE_ID;
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
        binding.btnBack.setOnClickListener(v ->
            requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        View.OnClickListener notImplementedListener = v -> showNotImplemented();
        binding.btnStopRide.setOnClickListener(notImplementedListener);
        binding.btnPanic.setOnClickListener(notImplementedListener);
        binding.btnReportIssue.setOnClickListener(notImplementedListener);
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
        activeRide = ride;
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
        boolean isCompleted = activeRide.status != null
            && !"ACTIVE".equalsIgnoreCase(activeRide.status);
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
        if (polylineAnnotationManager == null || activeRide == null || activeRide.route == null) {
            return;
        }
        List<Point> points = new ArrayList<>();
        for (RoutePoint routePoint : activeRide.route) {
            points.add(Point.fromLngLat(routePoint.lng, routePoint.lat));
        }
        polylineAnnotationManager.deleteAll();
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
        fetchRouteFromMapbox(activeRide.currentLocation, activeRide.destinationLocation, "driving-traffic", true);
    }

    private void fetchRouteFromMapbox(
        RoutePoint origin,
        RoutePoint destination,
        String profile,
        boolean allowFallback
    ) {
        if (directionsService == null) {
            fallbackToRoutePoints();
            return;
        }
        String coordinates = origin.lng + "," + origin.lat + ";" + destination.lng + "," + destination.lat;
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
                if (response.isSuccessful() && response.body() != null && hasRouteCoordinates(response.body())) {
                    updateRouteFromMapbox(response.body());
                } else if (allowFallback) {
                    fetchRouteFromMapbox(origin, destination, "driving", false);
                } else {
                    fallbackToRoutePoints();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxDirectionsResponse> call, @NonNull Throwable t) {
                if (allowFallback) {
                    fetchRouteFromMapbox(origin, destination, "driving", false);
                } else {
                    fallbackToRoutePoints();
                }
            }
        });
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
            activeRide.route = route;
            updateMapContent();
        }
    }

    private void fallbackToRoutePoints() {
        if (activeRide == null || activeRide.route == null || activeRide.route.isEmpty()) {
            return;
        }
        updateMapContent();
    }

    private void ensureAnnotationManagers() {
        if (mapView == null) {
            return;
        }
        AnnotationPlugin annotationPlugin = mapView.getAnnotations();
        if (pointAnnotationManager == null) {
            pointAnnotationManager = annotationPlugin.createPointAnnotationManager();
        }
        if (polylineAnnotationManager == null) {
            polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager();
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
        return safeModel + " • " + safePlate;
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
