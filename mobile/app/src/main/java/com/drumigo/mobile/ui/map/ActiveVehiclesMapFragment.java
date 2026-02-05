package com.drumigo.mobile.ui.map;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.VehicleApiService;
import com.drumigo.mobile.data.model.VehicleResponse;
import com.drumigo.mobile.databinding.FragmentActiveVehiclesMapBinding;
import com.google.gson.JsonObject;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActiveVehiclesMapFragment extends Fragment {

    private static final double DEFAULT_LNG = 19.8200;
    private static final double DEFAULT_LAT = 45.2500;
    private static final double DEFAULT_ZOOM = 12.5;
    private static final String MAPBOX_STYLE_URI = "mapbox://styles/mapbox/streets-v12";
    private static final long VEHICLE_POLLING_INTERVAL_MS = 10_000L;

    private FragmentActiveVehiclesMapBinding binding;
    private MapView mapView;
    private VehicleApiService vehicleApiService;
    private List<VehicleResponse> activeVehicles = new ArrayList<>();
    private boolean mapReady = false;
    private PointAnnotationManager pointAnnotationManager;
    private Bitmap availableIcon;
    private Bitmap busyIcon;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            fetchActiveVehicles();
            pollingHandler.postDelayed(this, VEHICLE_POLLING_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxOptions.setAccessToken(BuildConfig.MAPBOX_ACCESS_TOKEN);
        vehicleApiService = ApiClient.getVehicleApiService();
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentActiveVehiclesMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = binding.mapView;

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
            updateMarkers();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
        startPolling();
    }

    @Override
    public void onStop() {
        stopPolling();
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
        if (mapView != null) {
            mapView.onDestroy();
        }
        stopPolling();
        mapView = null;
        pointAnnotationManager = null;
        availableIcon = null;
        busyIcon = null;
        binding = null;
        super.onDestroyView();
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

    private void startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void fetchActiveVehicles() {
        if (vehicleApiService == null) {
            return;
        }
        vehicleApiService.getActiveVehicles().enqueue(new Callback<List<VehicleResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<List<VehicleResponse>> call,
                @NonNull Response<List<VehicleResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    showVehicleLoadError();
                    return;
                }
                activeVehicles = response.body();
                updateMarkers();
            }

            @Override
            public void onFailure(
                @NonNull Call<List<VehicleResponse>> call,
                @NonNull Throwable t
            ) {
                showVehicleLoadError();
            }
        });
    }

    private void updateMarkers() {
        if (!mapReady) {
            return;
        }
        ensureAnnotationManager();
        if (pointAnnotationManager == null) {
            return;
        }

        pointAnnotationManager.deleteAll();

        for (VehicleResponse vehicle : activeVehicles) {
            if (vehicle == null || vehicle.currentLat == null || vehicle.currentLng == null) {
                continue;
            }
            boolean isAvailable = vehicle.available == null || vehicle.available;
            Bitmap icon = isAvailable ? getAvailableIcon() : getBusyIcon();
            if (icon == null) {
                continue;
            }

            String driverFullName = buildDriverName(vehicle.driverName, vehicle.driverSurname);
            String statusLabel = isAvailable ? "Available" : "On a ride";

            JsonObject data = new JsonObject();
            data.addProperty("driverName", driverFullName);
            data.addProperty("status", statusLabel);

            PointAnnotationOptions options = new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(vehicle.currentLng, vehicle.currentLat))
                .withIconImage(icon)
                .withIconAnchor(IconAnchor.BOTTOM)
                .withData(data);
            pointAnnotationManager.create(options);
        }
    }

    private void showVehicleLoadError() {
        if (binding == null) {
            return;
        }
        Snackbar.make(
            binding.getRoot(),
            R.string.active_vehicles_load_failed,
            Snackbar.LENGTH_SHORT
        ).show();
    }

    private void ensureAnnotationManager() {
        if (mapView == null || pointAnnotationManager != null) {
            return;
        }
        AnnotationPlugin annotationPlugin = mapView.getAnnotations();
        pointAnnotationManager = annotationPlugin.createPointAnnotationManager();
        pointAnnotationManager.addClickListener(annotation -> {
            if (binding == null) {
                return false;
            }
            JsonObject data = annotation.getData() != null && annotation.getData().isJsonObject()
                ? annotation.getData().getAsJsonObject()
                : null;
            if (data != null) {
                String driverName = data.has("driverName") ? data.get("driverName").getAsString() : "";
                String status = data.has("status") ? data.get("status").getAsString() : "";
                String message = driverName.isEmpty() ? status : driverName + " • " + status;
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private Bitmap getAvailableIcon() {
        if (availableIcon == null) {
            availableIcon = createTintedMarker(R.color.success);
        }
        return availableIcon;
    }

    private Bitmap getBusyIcon() {
        if (busyIcon == null) {
            busyIcon = createTintedMarker(R.color.error);
        }
        return busyIcon;
    }

    private Bitmap createTintedMarker(int colorResId) {
        if (getContext() == null) {
            return null;
        }
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car);
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

    private String buildDriverName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }
}
