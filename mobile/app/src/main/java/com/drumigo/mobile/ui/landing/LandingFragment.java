package com.drumigo.mobile.ui.landing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.animation.ValueAnimator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.MapboxApiClient;
import com.drumigo.mobile.data.api.MapboxDirectionsService;
import com.drumigo.mobile.data.api.MapboxGeocodingService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.api.VehicleApiService;
import com.drumigo.mobile.data.model.VehicleResponse;
import com.drumigo.mobile.data.model.estimate.EstimateRequest;
import com.drumigo.mobile.data.model.estimate.EstimateResponse;
import com.drumigo.mobile.data.model.estimate.LocationDto;
import com.drumigo.mobile.data.model.mapbox.MapboxDirectionsResponse;
import com.drumigo.mobile.data.model.mapbox.MapboxGeocodingResponse;
import com.drumigo.mobile.databinding.FragmentLandingBinding;
import com.drumigo.mobile.ui.map.VehicleMarkerBitmapFactory;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationsUtils;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions;
import com.mapbox.maps.plugin.attribution.AttributionUtils;
import com.mapbox.maps.plugin.logo.LogoUtils;
import com.mapbox.maps.plugin.scalebar.ScaleBarUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandingFragment extends Fragment {

    private static final double DEFAULT_LNG = 19.8200;
    private static final double DEFAULT_LAT = 45.2500;
    private static final double DEFAULT_ZOOM = 12.5;
    private static final String MAPBOX_STYLE_URI = "mapbox://styles/mapbox/streets-v12";
    private static final long VEHICLE_POLLING_INTERVAL_MS = 10_000L;
    private static final long AUTOCOMPLETE_DEBOUNCE_MS = 320L;
    private static final int AUTOCOMPLETE_MIN_QUERY_LENGTH = 3;
    private static final int AUTOCOMPLETE_LIMIT = 6;
    private static final long ROUTE_CAMERA_ANIMATION_MS = 1600L;
    private static final List<Double> ROUTE_LABEL_ICON_OFFSET = Arrays.asList(0.0, -2.3);

    private FragmentLandingBinding binding;
    private MapView mapView;
    private VehicleApiService vehicleApiService;
    private RideApiService rideApiService;
    private MapboxGeocodingService geocodingService;
    private MapboxDirectionsService directionsService;

    private boolean mapReady = false;
    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private Bitmap availableIcon;
    private Bitmap busyIcon;
    private Bitmap routeStartLabelIcon;
    private Bitmap routeEndLabelIcon;
    private String routeStartLabelText;
    private String routeEndLabelText;
    private String lastRouteSignature = "";
    private boolean shouldCenterRoute = false;
    private boolean estimatePanelOpen = false;
    private float panelDragStartY = 0f;
    private LocationDto estimatedStartLocation;
    private LocationDto estimatedDestinationLocation;
    private ArrayAdapter<String> pickupSuggestionsAdapter;
    private ArrayAdapter<String> destinationSuggestionsAdapter;
    private int pickupAutocompleteToken = 0;
    private int destinationAutocompleteToken = 0;
    private int routeRequestToken = 0;
    private ValueAnimator routeCameraAnimator;

    private List<VehicleResponse> activeVehicles = new ArrayList<>();
    private List<List<Double>> routeCoordinates = Collections.emptyList();

    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private final Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private final Runnable pickupAutocompleteRunnable = this::triggerPickupAutocomplete;
    private final Runnable destinationAutocompleteRunnable = this::triggerDestinationAutocomplete;
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            fetchActiveVehicles();
            pollingHandler.postDelayed(this, VEHICLE_POLLING_INTERVAL_MS);
        }
    };

    private interface GeocodeCallback {
        void onSuccess(LocationDto location);
        void onFailure();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxOptions.setAccessToken(BuildConfig.MAPBOX_ACCESS_TOKEN);
        vehicleApiService = ApiClient.getVehicleApiService();
        rideApiService = ApiClient.getRideApiService();
        geocodingService = MapboxApiClient.getGeocodingService();
        directionsService = MapboxApiClient.getDirectionsService();
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLandingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = binding.mapView;

        setupHeaderActions();
        setupEstimatePanel();
        setupMap();
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
        stopPolling();
        autocompleteHandler.removeCallbacksAndMessages(null);
        if (mapView != null) {
            mapView.onDestroy();
        }
        mapView = null;
        pointAnnotationManager = null;
        polylineAnnotationManager = null;
        availableIcon = null;
        busyIcon = null;
        routeStartLabelIcon = null;
        routeEndLabelIcon = null;
        routeStartLabelText = null;
        routeEndLabelText = null;
        lastRouteSignature = "";
        shouldCenterRoute = false;
        if (routeCameraAnimator != null) {
            routeCameraAnimator.cancel();
            routeCameraAnimator = null;
        }
        estimatedStartLocation = null;
        estimatedDestinationLocation = null;
        pickupSuggestionsAdapter = null;
        destinationSuggestionsAdapter = null;
        binding = null;
        super.onDestroyView();
    }

    private void setupHeaderActions() {
        binding.btnBookRide.setOnClickListener(v -> navigateTo(R.id.registrationFragment));
    }

    private void setupEstimatePanel() {
        String[] vehicleLabels = getResources().getStringArray(R.array.landing_vehicle_type_labels);
        ArrayAdapter<String> vehicleTypeAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            vehicleLabels
        );
        pickupSuggestionsAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            new ArrayList<>()
        );
        destinationSuggestionsAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            new ArrayList<>()
        );
        binding.pickupInput.setAdapter(pickupSuggestionsAdapter);
        binding.destinationInput.setAdapter(destinationSuggestionsAdapter);
        setupLocationAutocomplete();

        binding.vehicleTypeDropdown.setAdapter(vehicleTypeAdapter);
        if (vehicleLabels.length > 0) {
            binding.vehicleTypeDropdown.setText(vehicleLabels[0], false);
        }
        binding.vehicleTypeDropdown.setKeyListener(null);
        binding.vehicleTypeDropdown.setOnClickListener(v -> binding.vehicleTypeDropdown.showDropDown());

        binding.btnEstimate.setOnClickListener(v -> calculateEstimate());
        binding.btnEstimateLauncher.setOnClickListener(v -> openEstimatePanel());
        binding.btnCloseEstimatePanel.setOnClickListener(v -> closeEstimatePanel());
        setupPanelSwipeToClose();
        initializeEstimatePanel();
        updateActiveVehicleCount();
    }

    private void setupLocationAutocomplete() {
        binding.pickupInput.setOnItemClickListener((parent, view, position, id) -> {
            autocompleteHandler.removeCallbacks(pickupAutocompleteRunnable);
            binding.pickupInput.dismissDropDown();
        });
        binding.destinationInput.setOnItemClickListener((parent, view, position, id) -> {
            autocompleteHandler.removeCallbacks(destinationAutocompleteRunnable);
            binding.destinationInput.dismissDropDown();
        });

        binding.pickupInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleAutocompleteForPickup();
            }
        });

        binding.destinationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleAutocompleteForDestination();
            }
        });
    }

    private void scheduleAutocompleteForPickup() {
        autocompleteHandler.removeCallbacks(pickupAutocompleteRunnable);
        autocompleteHandler.postDelayed(pickupAutocompleteRunnable, AUTOCOMPLETE_DEBOUNCE_MS);
    }

    private void scheduleAutocompleteForDestination() {
        autocompleteHandler.removeCallbacks(destinationAutocompleteRunnable);
        autocompleteHandler.postDelayed(destinationAutocompleteRunnable, AUTOCOMPLETE_DEBOUNCE_MS);
    }

    private void triggerPickupAutocomplete() {
        if (binding == null) {
            return;
        }
        String query = binding.pickupInput.getText() == null
            ? ""
            : binding.pickupInput.getText().toString().trim();
        fetchAddressSuggestions(query, true);
    }

    private void triggerDestinationAutocomplete() {
        if (binding == null) {
            return;
        }
        String query = binding.destinationInput.getText() == null
            ? ""
            : binding.destinationInput.getText().toString().trim();
        fetchAddressSuggestions(query, false);
    }

    private void fetchAddressSuggestions(String query, boolean isPickup) {
        if (binding == null || geocodingService == null) {
            return;
        }
        if (query.length() < AUTOCOMPLETE_MIN_QUERY_LENGTH) {
            updateSuggestionAdapter(isPickup, new ArrayList<>());
            return;
        }
        int token = isPickup ? ++pickupAutocompleteToken : ++destinationAutocompleteToken;
        String encodedQuery = Uri.encode(query);
        geocodingService.searchAddressSuggestions(
            encodedQuery,
            true,
            AUTOCOMPLETE_LIMIT,
            "address,place,poi",
            BuildConfig.MAPBOX_ACCESS_TOKEN
        ).enqueue(new Callback<MapboxGeocodingResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<MapboxGeocodingResponse> call,
                @NonNull Response<MapboxGeocodingResponse> response
            ) {
                if (binding == null || !isLatestAutocompleteToken(token, isPickup)) {
                    return;
                }
                List<String> suggestions = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().features != null) {
                    for (MapboxGeocodingResponse.Feature feature : response.body().features) {
                        if (feature == null || feature.place_name == null
                            || feature.place_name.trim().isEmpty()) {
                            continue;
                        }
                        suggestions.add(feature.place_name);
                    }
                }
                updateSuggestionAdapter(isPickup, suggestions);
            }

            @Override
            public void onFailure(@NonNull Call<MapboxGeocodingResponse> call, @NonNull Throwable t) {
                if (binding == null || !isLatestAutocompleteToken(token, isPickup)) {
                    return;
                }
                updateSuggestionAdapter(isPickup, new ArrayList<>());
            }
        });
    }

    private boolean isLatestAutocompleteToken(int token, boolean isPickup) {
        return isPickup ? token == pickupAutocompleteToken : token == destinationAutocompleteToken;
    }

    private void updateSuggestionAdapter(boolean isPickup, List<String> suggestions) {
        if (binding == null) {
            return;
        }
        ArrayAdapter<String> adapter = isPickup ? pickupSuggestionsAdapter : destinationSuggestionsAdapter;
        if (adapter == null) {
            return;
        }
        adapter.clear();
        adapter.addAll(suggestions);
        adapter.notifyDataSetChanged();

        if (suggestions.isEmpty()) {
            if (isPickup) {
                binding.pickupInput.dismissDropDown();
            } else {
                binding.destinationInput.dismissDropDown();
            }
            return;
        }

        if (isPickup) {
            if (binding.pickupInput.hasFocus()) {
                binding.pickupInput.showDropDown();
            }
        } else {
            if (binding.destinationInput.hasFocus()) {
                binding.destinationInput.showDropDown();
            }
        }
    }

    private void setupPanelSwipeToClose() {
        View.OnTouchListener dragListener = (v, event) -> {
            if (binding == null || !estimatePanelOpen) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    panelDragStartY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - panelDragStartY;
                    if (deltaY > 0f) {
                        binding.estimatePanel.setTranslationY(deltaY);
                        float alpha = 1f - Math.min(0.12f, deltaY / 2200f);
                        binding.estimatePanel.setAlpha(alpha);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float draggedDistance = binding.estimatePanel.getTranslationY();
                    if (draggedDistance > 140f) {
                        closeEstimatePanel();
                    } else {
                        binding.estimatePanel.animate()
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
        binding.estimateDragHandle.setOnTouchListener(dragListener);
        binding.estimateHeader.setOnTouchListener(dragListener);
    }

    private void initializeEstimatePanel() {
        if (binding == null) {
            return;
        }
        estimatePanelOpen = false;
        binding.estimatePanel.setVisibility(View.GONE);
        binding.btnEstimateLauncher.setVisibility(View.VISIBLE);
        binding.estimatePanel.setAlpha(1f);
        binding.estimatePanel.setTranslationY(0f);
    }

    private void openEstimatePanel() {
        if (binding == null || estimatePanelOpen) {
            return;
        }
        estimatePanelOpen = true;
        adjustPanelHeightForScreen();
        binding.btnEstimateLauncher.setVisibility(View.GONE);
        binding.estimatePanel.setVisibility(View.VISIBLE);
        binding.estimatePanel.post(() -> {
            if (binding == null) {
                return;
            }
            float startOffset = binding.getRoot().getHeight();
            binding.estimatePanel.setTranslationY(startOffset);
            binding.estimatePanel.setAlpha(0.96f);
            binding.estimatePanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(240L)
                .start();
        });
    }

    private void closeEstimatePanel() {
        if (binding == null || !estimatePanelOpen) {
            return;
        }
        estimatePanelOpen = false;
        float endOffset = binding.getRoot().getHeight();
        binding.estimatePanel.animate()
            .translationY(endOffset)
            .alpha(0.96f)
            .setDuration(200L)
            .withEndAction(() -> {
                if (binding == null) {
                    return;
                }
                binding.estimatePanel.setVisibility(View.GONE);
                binding.estimatePanel.setTranslationY(0f);
                binding.estimatePanel.setAlpha(1f);
                binding.btnEstimateLauncher.setVisibility(View.VISIBLE);
            })
            .start();
    }

    private void adjustPanelHeightForScreen() {
        if (binding == null) {
            return;
        }
        int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
        int maxPanelHeight = (int) (screenHeight * 0.82f);

        ViewGroup.LayoutParams panelLayoutParams = binding.estimatePanel.getLayoutParams();
        panelLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        binding.estimatePanel.setLayoutParams(panelLayoutParams);

        ViewGroup.LayoutParams scrollLayoutParams = binding.estimateScroll.getLayoutParams();
        scrollLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        binding.estimateScroll.setLayoutParams(scrollLayoutParams);

        binding.estimatePanel.post(() -> {
            if (binding == null) {
                return;
            }
            if (binding.estimatePanel.getHeight() <= maxPanelHeight) {
                return;
            }
            int headerHeight = binding.estimateDragHandle.getHeight()
                + binding.estimateHeader.getHeight()
                + dpToPx(20);
            int desiredScrollHeight = Math.max(dpToPx(220), maxPanelHeight - headerHeight);
            ViewGroup.LayoutParams innerScrollParams = binding.estimateScroll.getLayoutParams();
            innerScrollParams.height = desiredScrollHeight;
            binding.estimateScroll.setLayoutParams(innerScrollParams);
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
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
                    return;
                }
                activeVehicles = response.body();
                updateActiveVehicleCount();
                updateMapContent();
            }

            @Override
            public void onFailure(@NonNull Call<List<VehicleResponse>> call, @NonNull Throwable t) {
            }
        });
    }

    private void calculateEstimate() {
        if (binding == null) {
            return;
        }
        binding.pickupInput.dismissDropDown();
        binding.destinationInput.dismissDropDown();
        if (isMapboxTokenMissing()) {
            showMapError();
            showEstimateError(R.string.mapbox_token_missing);
            return;
        }

        String startAddress = binding.pickupInput.getText() == null
            ? ""
            : binding.pickupInput.getText().toString().trim();
        String destinationAddress = binding.destinationInput.getText() == null
            ? ""
            : binding.destinationInput.getText().toString().trim();

        if (startAddress.isEmpty() || destinationAddress.isEmpty()) {
            showEstimateError(R.string.landing_locations_required);
            return;
        }

        setEstimateLoading(true);
        geocodeAddress(startAddress, new GeocodeCallback() {
            @Override
            public void onSuccess(LocationDto startLocation) {
                geocodeAddress(destinationAddress, new GeocodeCallback() {
                    @Override
                    public void onSuccess(LocationDto destinationLocation) {
                        requestEstimate(startLocation, destinationLocation);
                    }

                    @Override
                    public void onFailure() {
                        setEstimateLoading(false);
                        showEstimateError(R.string.landing_geocode_failed);
                    }
                });
            }

            @Override
            public void onFailure() {
                setEstimateLoading(false);
                showEstimateError(R.string.landing_geocode_failed);
            }
        });
    }

    private void requestEstimate(LocationDto startLocation, LocationDto destinationLocation) {
        if (rideApiService == null) {
            setEstimateLoading(false);
            showEstimateError(R.string.landing_estimate_failed);
            return;
        }

        EstimateRequest request = new EstimateRequest(
            startLocation,
            destinationLocation,
            new ArrayList<>(),
            getSelectedVehicleType()
        );

        rideApiService.estimateRide(request).enqueue(new Callback<EstimateResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<EstimateResponse> call,
                @NonNull Response<EstimateResponse> response
            ) {
                setEstimateLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showEstimateError(R.string.landing_estimate_failed);
                    return;
                }

                EstimateResponse estimateResponse = response.body();
                estimatedStartLocation = startLocation;
                estimatedDestinationLocation = destinationLocation;
                showEstimateResult(estimateResponse);
                requestStreetRoute(
                    startLocation,
                    destinationLocation,
                    estimateResponse.routeCoordinates == null
                        ? Collections.emptyList()
                        : estimateResponse.routeCoordinates
                );
            }

            @Override
            public void onFailure(@NonNull Call<EstimateResponse> call, @NonNull Throwable t) {
                setEstimateLoading(false);
                showEstimateError(R.string.landing_estimate_failed);
            }
        });
    }

    private void requestStreetRoute(
        LocationDto startLocation,
        LocationDto destinationLocation,
        List<List<Double>> fallbackCoordinates
    ) {
        if (startLocation == null
            || destinationLocation == null
            || startLocation.latitude == null
            || startLocation.longitude == null
            || destinationLocation.latitude == null
            || destinationLocation.longitude == null) {
            applyRouteCoordinates(fallbackCoordinates, ++routeRequestToken);
            return;
        }

        int requestToken = ++routeRequestToken;
        String coordinates = startLocation.longitude + "," + startLocation.latitude
            + ";" + destinationLocation.longitude + "," + destinationLocation.latitude;
        fetchDirectionsWithProfile(
            "driving-traffic",
            coordinates,
            fallbackCoordinates,
            requestToken,
            true
        );
    }

    private void fetchDirectionsWithProfile(
        String profile,
        String coordinates,
        List<List<Double>> fallbackCoordinates,
        int requestToken,
        boolean allowRetryWithDriving
    ) {
        if (directionsService == null) {
            applyRouteCoordinates(fallbackCoordinates, requestToken);
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
                if (response.isSuccessful()
                    && response.body() != null
                    && response.body().routes != null
                    && !response.body().routes.isEmpty()
                    && response.body().routes.get(0).geometry != null
                    && response.body().routes.get(0).geometry.coordinates != null
                    && response.body().routes.get(0).geometry.coordinates.size() >= 2) {
                    applyRouteCoordinates(response.body().routes.get(0).geometry.coordinates, requestToken);
                    return;
                }
                if (allowRetryWithDriving) {
                    fetchDirectionsWithProfile(
                        "driving",
                        coordinates,
                        fallbackCoordinates,
                        requestToken,
                        false
                    );
                } else {
                    applyRouteCoordinates(fallbackCoordinates, requestToken);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxDirectionsResponse> call, @NonNull Throwable t) {
                if (requestToken != routeRequestToken) {
                    return;
                }
                if (allowRetryWithDriving) {
                    fetchDirectionsWithProfile(
                        "driving",
                        coordinates,
                        fallbackCoordinates,
                        requestToken,
                        false
                    );
                } else {
                    applyRouteCoordinates(fallbackCoordinates, requestToken);
                }
            }
        });
    }

    private void applyRouteCoordinates(List<List<Double>> coordinates, int requestToken) {
        if (requestToken != routeRequestToken) {
            return;
        }
        routeCoordinates = coordinates == null ? Collections.emptyList() : coordinates;
        shouldCenterRoute = true;
        updateMapContent();
    }

    private void geocodeAddress(String address, GeocodeCallback callback) {
        if (geocodingService == null) {
            callback.onFailure();
            return;
        }

        String encodedAddress = Uri.encode(address);
        geocodingService.geocodeAddress(encodedAddress, 1, BuildConfig.MAPBOX_ACCESS_TOKEN)
            .enqueue(new Callback<MapboxGeocodingResponse>() {
                @Override
                public void onResponse(
                    @NonNull Call<MapboxGeocodingResponse> call,
                    @NonNull Response<MapboxGeocodingResponse> response
                ) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onFailure();
                        return;
                    }

                    MapboxGeocodingResponse geocodingResponse = response.body();
                    if (geocodingResponse.features == null || geocodingResponse.features.isEmpty()) {
                        callback.onFailure();
                        return;
                    }

                    MapboxGeocodingResponse.Feature firstFeature = geocodingResponse.features.get(0);
                    if (firstFeature.center == null || firstFeature.center.size() < 2) {
                        callback.onFailure();
                        return;
                    }

                    double lng = firstFeature.center.get(0);
                    double lat = firstFeature.center.get(1);
                    String normalizedAddress = firstFeature.place_name == null
                        ? address
                        : firstFeature.place_name;
                    callback.onSuccess(new LocationDto(lat, lng, normalizedAddress));
                }

                @Override
                public void onFailure(@NonNull Call<MapboxGeocodingResponse> call, @NonNull Throwable t) {
                    callback.onFailure();
                }
            });
    }

    private void showEstimateResult(EstimateResponse response) {
        if (binding == null || response == null) {
            return;
        }

        int durationMinutes = response.durationInMinutes == null ? 0 : response.durationInMinutes;
        double estimatedPrice = response.estimatedPrice == null ? 0.0 : response.estimatedPrice;
        double distance = response.distanceInKm == null ? 0.0 : response.distanceInKm;

        binding.resultEtaValue.setText(getString(R.string.landing_estimate_eta_value_format, durationMinutes));
        binding.resultPriceValue.setText(getString(R.string.landing_estimate_price_value_format, estimatedPrice));
        binding.resultDistanceValue.setText(getString(R.string.landing_estimate_distance_value_format, distance));
        binding.estimateResults.setVisibility(View.VISIBLE);
    }

    private void setEstimateLoading(boolean loading) {
        if (binding == null) {
            return;
        }
        binding.btnEstimate.setEnabled(!loading);
        binding.estimateProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateMapContent() {
        if (!mapReady) {
            return;
        }
        ensureAnnotationManagers();
        updateVehicleMarkers();
        updateRouteLine();
        centerMapOnRouteIfPresent();
    }

    private void ensureAnnotationManagers() {
        if (mapView == null) {
            return;
        }
        AnnotationPlugin annotationPlugin = AnnotationsUtils.getAnnotations(mapView);
        if (polylineAnnotationManager == null) {
            polylineAnnotationManager = PolylineAnnotationManagerKt.createPolylineAnnotationManager(annotationPlugin, null);
        }
        if (pointAnnotationManager == null) {
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, null);
        }
    }

    private void updateVehicleMarkers() {
        if (pointAnnotationManager == null) {
            return;
        }
        pointAnnotationManager.deleteAll();
        for (VehicleResponse vehicle : activeVehicles) {
            if (vehicle == null || vehicle.currentLat == null || vehicle.currentLng == null) {
                continue;
            }
            Bitmap icon = (vehicle.available == null || vehicle.available)
                ? getAvailableIcon()
                : getBusyIcon();
            if (icon == null) {
                continue;
            }
            pointAnnotationManager.create(new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(vehicle.currentLng, vehicle.currentLat))
                .withIconImage(icon)
                .withIconAnchor(IconAnchor.BOTTOM));
        }
        addRouteEndpointLabels();
    }

    private void addRouteEndpointLabels() {
        if (pointAnnotationManager == null) {
            return;
        }
        List<Point> routePoints = buildRenderableRoutePoints();
        if (routePoints.size() < 2) {
            return;
        }

        Point startPoint = routePoints.get(0);
        Point endPoint = routePoints.get(routePoints.size() - 1);
        if (estimatedStartLocation != null && estimatedStartLocation.longitude != null
            && estimatedStartLocation.latitude != null) {
            startPoint = Point.fromLngLat(estimatedStartLocation.longitude, estimatedStartLocation.latitude);
        }
        if (estimatedDestinationLocation != null && estimatedDestinationLocation.longitude != null
            && estimatedDestinationLocation.latitude != null) {
            endPoint = Point.fromLngLat(estimatedDestinationLocation.longitude, estimatedDestinationLocation.latitude);
        }

        String startLabel = compactAddressLabel(
            estimatedStartLocation != null ? estimatedStartLocation.address : null,
            getString(R.string.landing_route_start_label)
        );
        String endLabel = compactAddressLabel(
            estimatedDestinationLocation != null ? estimatedDestinationLocation.address : null,
            getString(R.string.landing_route_end_label)
        );

        Bitmap startLabelBitmap = getRouteEndpointLabelIcon(
            true,
            startLabel,
            ContextCompat.getColor(requireContext(), R.color.primary)
        );
        Bitmap endLabelBitmap = getRouteEndpointLabelIcon(
            false,
            endLabel,
            ContextCompat.getColor(requireContext(), R.color.accent)
        );

        if (startLabelBitmap != null) {
            pointAnnotationManager.create(new PointAnnotationOptions()
                .withPoint(startPoint)
                .withIconImage(startLabelBitmap)
                .withIconAnchor(IconAnchor.BOTTOM)
                .withIconOffset(ROUTE_LABEL_ICON_OFFSET));
        }
        if (endLabelBitmap != null) {
            pointAnnotationManager.create(new PointAnnotationOptions()
                .withPoint(endPoint)
                .withIconImage(endLabelBitmap)
                .withIconAnchor(IconAnchor.BOTTOM)
                .withIconOffset(ROUTE_LABEL_ICON_OFFSET));
        }
    }

    private boolean isCoordinatePairValid(List<Double> pair) {
        return pair != null && pair.size() >= 2 && pair.get(0) != null && pair.get(1) != null;
    }

    private void updateRouteLine() {
        if (polylineAnnotationManager == null) {
            return;
        }
        List<Point> points = buildRenderableRoutePoints();
        if (points.size() < 2) {
            if (!lastRouteSignature.isEmpty()) {
                polylineAnnotationManager.deleteAll();
                lastRouteSignature = "";
            }
            return;
        }

        String signature = buildRouteSignature(points);
        if (signature.equals(lastRouteSignature)) {
            return;
        }
        polylineAnnotationManager.deleteAll();
        polylineAnnotationManager.create(new PolylineAnnotationOptions()
            .withPoints(points)
            .withLineColor("#5B4CDB")
            .withLineWidth(5.0));
        lastRouteSignature = signature;
    }

    private void centerMapOnRouteIfPresent() {
        if (!shouldCenterRoute || mapView == null) {
            return;
        }
        List<Point> points = buildRenderableRoutePoints();
        if (points.size() < 2) {
            return;
        }
        Point first = points.get(0);
        Point last = points.get(points.size() - 1);
        double centerLng = (first.longitude() + last.longitude()) / 2.0;
        double centerLat = (first.latitude() + last.latitude()) / 2.0;
        animateCameraTo(centerLng, centerLat, DEFAULT_ZOOM + 0.6);
        shouldCenterRoute = false;
    }

    private void animateCameraTo(double targetLng, double targetLat, double targetZoom) {
        if (mapView == null) {
            return;
        }
        if (routeCameraAnimator != null) {
            routeCameraAnimator.cancel();
        }
        double startLng = mapView.getMapboxMap().getCameraState().getCenter().longitude();
        double startLat = mapView.getMapboxMap().getCameraState().getCenter().latitude();
        double startZoom = mapView.getMapboxMap().getCameraState().getZoom();

        routeCameraAnimator = ValueAnimator.ofFloat(0f, 1f);
        routeCameraAnimator.setDuration(ROUTE_CAMERA_ANIMATION_MS);
        routeCameraAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        routeCameraAnimator.addUpdateListener(animation -> {
            if (mapView == null) {
                return;
            }
            float t = (float) animation.getAnimatedValue();
            double lng = startLng + (targetLng - startLng) * t;
            double lat = startLat + (targetLat - startLat) * t;
            double zoom = startZoom + (targetZoom - startZoom) * t;
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(zoom)
                .build());
        });
        routeCameraAnimator.start();
    }

    private String buildRouteSignature(List<Point> points) {
        StringBuilder sb = new StringBuilder();
        sb.append(points.size()).append('|');
        if (!points.isEmpty()) {
            Point first = points.get(0);
            Point last = points.get(points.size() - 1);
            sb.append(Math.round(first.longitude() * 1_000_000)).append(',')
                .append(Math.round(first.latitude() * 1_000_000)).append('|')
                .append(Math.round(last.longitude() * 1_000_000)).append(',')
                .append(Math.round(last.latitude() * 1_000_000));
        }
        return sb.toString();
    }

    private List<Point> buildRenderableRoutePoints() {
        List<Point> points = new ArrayList<>();
        if (routeCoordinates != null && !routeCoordinates.isEmpty()) {
            boolean swapOrder = shouldSwapCoordinateOrder(routeCoordinates);
            for (List<Double> pair : routeCoordinates) {
                if (!isCoordinatePairValid(pair)) {
                    continue;
                }
                double lng = swapOrder ? pair.get(1) : pair.get(0);
                double lat = swapOrder ? pair.get(0) : pair.get(1);
                if (!isLatLngInBounds(lat, lng)) {
                    continue;
                }
                points.add(Point.fromLngLat(lng, lat));
            }
        }
        return points;
    }

    private boolean shouldSwapCoordinateOrder(List<List<Double>> coordinates) {
        if (estimatedStartLocation == null
            || estimatedStartLocation.longitude == null
            || estimatedStartLocation.latitude == null) {
            return false;
        }
        for (List<Double> pair : coordinates) {
            if (!isCoordinatePairValid(pair)) {
                continue;
            }
            double aLng = pair.get(0);
            double aLat = pair.get(1);
            double bLng = pair.get(1);
            double bLat = pair.get(0);
            double expectedLng = estimatedStartLocation.longitude;
            double expectedLat = estimatedStartLocation.latitude;
            double normalDistance = square(aLng - expectedLng) + square(aLat - expectedLat);
            double swappedDistance = square(bLng - expectedLng) + square(bLat - expectedLat);
            return swappedDistance + 0.000001 < normalDistance;
        }
        return false;
    }

    private boolean isLatLngInBounds(double lat, double lng) {
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }

    private double square(double value) {
        return value * value;
    }

    private Bitmap getAvailableIcon() {
        if (availableIcon == null) {
            availableIcon = createVehicleMarker(R.color.marker_available);
        }
        return availableIcon;
    }

    private Bitmap getBusyIcon() {
        if (busyIcon == null) {
            busyIcon = createVehicleMarker(R.color.marker_busy);
        }
        return busyIcon;
    }

    private Bitmap createVehicleMarker(int markerColorResId) {
        if (getContext() == null) {
            return null;
        }
        return VehicleMarkerBitmapFactory.createStatusPin(
            requireContext(),
            R.drawable.ic_car,
            ContextCompat.getColor(requireContext(), markerColorResId),
            ContextCompat.getColor(requireContext(), R.color.white)
        );
    }

    private Bitmap getRouteEndpointLabelIcon(boolean isStart, String label, int accentColor) {
        if (isStart) {
            if (routeStartLabelIcon == null || routeStartLabelText == null || !routeStartLabelText.equals(label)) {
                routeStartLabelText = label;
                routeStartLabelIcon = createRouteEndpointLabelBitmap(label, accentColor);
            }
            return routeStartLabelIcon;
        }
        if (routeEndLabelIcon == null || routeEndLabelText == null || !routeEndLabelText.equals(label)) {
            routeEndLabelText = label;
            routeEndLabelIcon = createRouteEndpointLabelBitmap(label, accentColor);
        }
        return routeEndLabelIcon;
    }

    private Bitmap createRouteEndpointLabelBitmap(String label, int accentColor) {
        if (getContext() == null) {
            return null;
        }
        float density = requireContext().getResources().getDisplayMetrics().density;
        float bubbleHeight = 30f * density;
        float tailHeight = 8f * density;
        float radius = 11f * density;
        float strokeWidth = 1f * density;
        float horizontalPadding = 12f * density;
        float dotRadius = 3.5f * density;
        float textGap = 7f * density;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        textPaint.setTextSize(12f * density);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float textWidth = textPaint.measureText(label);

        int width = Math.round(Math.max(96f * density, horizontalPadding * 2f + dotRadius * 2f + textGap + textWidth));
        int height = Math.round(bubbleHeight + tailHeight);
        float centerX = width / 2f;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF bubbleRect = new RectF(0f, 0f, width, bubbleHeight);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(35, 31, 41, 55));
        canvas.drawRoundRect(
            bubbleRect.left,
            bubbleRect.top + (1.5f * density),
            bubbleRect.right,
            bubbleRect.bottom + (1.5f * density),
            radius,
            radius,
            shadowPaint
        );

        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setColor(ContextCompat.getColor(requireContext(), R.color.white));
        canvas.drawRoundRect(bubbleRect, radius, radius, bubblePaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(Color.argb(45, 31, 31, 31));
        canvas.drawRoundRect(bubbleRect, radius, radius, strokePaint);

        Path tail = new Path();
        tail.moveTo(centerX - (6f * density), bubbleHeight - strokeWidth);
        tail.lineTo(centerX + (6f * density), bubbleHeight - strokeWidth);
        tail.lineTo(centerX, bubbleHeight + tailHeight);
        tail.close();
        canvas.drawPath(tail, bubblePaint);
        canvas.drawPath(tail, strokePaint);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(accentColor);
        float dotCenterX = horizontalPadding + dotRadius;
        float dotCenterY = bubbleHeight / 2f;
        canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textX = dotCenterX + dotRadius + textGap;
        float textY = bubbleHeight / 2f - ((fontMetrics.ascent + fontMetrics.descent) / 2f);
        canvas.drawText(label, textX, textY, textPaint);
        return bitmap;
    }

    private String compactAddressLabel(String address, String fallback) {
        if (address == null || address.trim().isEmpty()) {
            return fallback;
        }
        String cleaned = address.trim();
        int commaIndex = cleaned.indexOf(',');
        if (commaIndex > 0) {
            cleaned = cleaned.substring(0, commaIndex).trim();
        }
        if (cleaned.length() > 26) {
            return cleaned.substring(0, 25) + "...";
        }
        return cleaned;
    }

    private String getSelectedVehicleType() {
        if (binding == null) {
            return "STANDARD";
        }
        String selectedLabel = binding.vehicleTypeDropdown.getText() == null
            ? ""
            : binding.vehicleTypeDropdown.getText().toString().trim();
        String[] labelValues = getResources().getStringArray(R.array.landing_vehicle_type_labels);
        String[] typeValues = getResources().getStringArray(R.array.landing_vehicle_type_values);
        for (int i = 0; i < labelValues.length && i < typeValues.length; i++) {
            if (labelValues[i].equalsIgnoreCase(selectedLabel)) {
                return typeValues[i];
            }
        }
        return typeValues.length > 0 ? typeValues[0] : "STANDARD";
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

    private void updateActiveVehicleCount() {
        if (binding == null) {
            return;
        }
        binding.activeVehiclesCount.setText(
            getString(R.string.landing_active_vehicles_count, activeVehicles.size())
        );
    }

    private void showEstimateError(int messageResId) {
        if (binding == null) {
            return;
        }
        Snackbar.make(binding.getRoot(), messageResId, Snackbar.LENGTH_SHORT).show();
    }

    private void navigateTo(int destinationId) {
        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() != null
            && navController.getCurrentDestination().getId() == destinationId) {
            return;
        }
        navController.navigate(destinationId);
    }
}
