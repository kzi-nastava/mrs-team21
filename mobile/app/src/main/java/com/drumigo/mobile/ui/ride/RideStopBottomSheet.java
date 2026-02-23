package com.drumigo.mobile.ui.ride;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.MapboxApiClient;
import com.drumigo.mobile.data.api.MapboxGeocodingService;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.mapbox.MapboxGeocodingResponse;
import com.drumigo.mobile.data.model.ride.ActiveRide;
import com.drumigo.mobile.data.model.ride.RideDetailsResponse;
import com.drumigo.mobile.data.model.ride.RideResponse;
import com.drumigo.mobile.data.model.ride.RideStopRequest;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;
import com.drumigo.mobile.data.model.ride.RoutePoint;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RideStopBottomSheet {

    interface CallbackHandler {
        void onRideStopped(@NonNull RideResponse rideResponse);
    }

    private final Context context;
    private final RideApiService rideApiService;
    private final MapboxGeocodingService geocodingService;
    private final long rideId;
    private final ActiveRide activeRide;
    private final CallbackHandler callbackHandler;

    private BottomSheetDialog dialog;
    private TextInputEditText stopAddressInput;
    private TextView stopErrorText;
    private TextView stopPassengerAvatarView;
    private TextView stopPassengerNameView;
    private TextView stopPassengerMetaView;
    private TextView stopPickupAddressView;
    private TextView stopDestinationAddressView;
    private TextView stopDurationValueView;
    private TextView stopDistanceValueView;
    private MaterialButton stopConfirmButton;
    private MaterialButton stopContinueButton;
    private boolean submitting = false;
    private boolean reverseGeocodeInFlight = false;
    private String lastReverseGeocodeKey = "";
    private String resolvedCurrentAddress = "";

    private RideStopBottomSheet(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @NonNull ActiveRide activeRide,
        @NonNull CallbackHandler callbackHandler
    ) {
        this.context = context;
        this.rideApiService = rideApiService;
        this.geocodingService = MapboxApiClient.getGeocodingService();
        this.rideId = rideId;
        this.activeRide = activeRide;
        this.callbackHandler = callbackHandler;
    }

    static void show(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @NonNull ActiveRide activeRide,
        @NonNull CallbackHandler callbackHandler
    ) {
        new RideStopBottomSheet(
            context,
            rideApiService,
            rideId,
            activeRide,
            callbackHandler
        ).showInternal();
    }

    private void showInternal() {
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_stop);
        dialog.setDismissWithAnimation(true);

        bindViews();
        bindRideData();
        fetchRideTracking();
        fetchRideDetails();
        bindActions();

        dialog.setOnShowListener(ignored -> configureBottomSheet());
        dialog.show();
    }

    private void bindViews() {
        if (dialog == null) {
            return;
        }
        stopAddressInput = dialog.findViewById(R.id.stopAddressInput);
        stopErrorText = dialog.findViewById(R.id.stopErrorText);
        stopPassengerAvatarView = dialog.findViewById(R.id.stopPassengerAvatar);
        stopPassengerNameView = dialog.findViewById(R.id.stopPassengerName);
        stopPassengerMetaView = dialog.findViewById(R.id.stopPassengerRating);
        stopPickupAddressView = dialog.findViewById(R.id.stopPickupAddress);
        stopDestinationAddressView = dialog.findViewById(R.id.stopDestinationAddress);
        stopDurationValueView = dialog.findViewById(R.id.stopDurationValue);
        stopDistanceValueView = dialog.findViewById(R.id.stopDistanceValue);
        stopConfirmButton = dialog.findViewById(R.id.stopConfirmButton);
        stopContinueButton = dialog.findViewById(R.id.stopContinueButton);
    }

    private void bindRideData() {
        if (dialog == null) {
            return;
        }
        bindPassengerInfo(
            context.getString(R.string.ride_tracking_stop_passenger_unknown),
            context.getString(R.string.ride_tracking_stop_passenger_meta_unknown)
        );
        if (stopPickupAddressView != null) {
            stopPickupAddressView.setText(safe(activeRide.startAddress, context.getString(R.string.ride_tracking_from_placeholder)));
        }
        if (stopDestinationAddressView != null) {
            stopDestinationAddressView.setText(safe(activeRide.destinationAddress, context.getString(R.string.ride_tracking_to_placeholder)));
        }
        if (stopDurationValueView != null) {
            stopDurationValueView.setText(formatDuration(activeRide.estimatedArrivalTimeSec));
        }
        if (stopDistanceValueView != null) {
            stopDistanceValueView.setText(formatDistance(activeRide.totalDistanceKm));
        }
        prefillStopAddressIfEmpty();
    }

    private void fetchRideTracking() {
        rideApiService.getRideTracking(rideId).enqueue(new Callback<RideTrackingResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideTrackingResponse> call,
                @NonNull Response<RideTrackingResponse> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                RideTrackingResponse tracking = response.body();
                if (tracking.estimatedDurationSec != null) {
                    activeRide.estimatedArrivalTimeSec = tracking.estimatedDurationSec;
                }
                if (tracking.totalDistanceKm != null) {
                    activeRide.totalDistanceKm = tracking.totalDistanceKm;
                }
                if (tracking.vehicleCurrentLat != null && tracking.vehicleCurrentLng != null) {
                    activeRide.currentLocation = new RoutePoint(tracking.vehicleCurrentLat, tracking.vehicleCurrentLng, 0);
                }
                if (tracking.waypoints != null && !tracking.waypoints.isEmpty()) {
                    List<RideTrackingResponse.WaypointInfo> waypoints = new ArrayList<>(tracking.waypoints);
                    Collections.sort(waypoints, Comparator.comparingInt(wp -> wp.order == null ? 0 : wp.order));
                    RideTrackingResponse.WaypointInfo first = waypoints.get(0);
                    RideTrackingResponse.WaypointInfo last = waypoints.get(waypoints.size() - 1);
                    if (first != null && first.address != null && !first.address.trim().isEmpty()) {
                        activeRide.startAddress = first.address;
                    }
                    if (last != null && last.address != null && !last.address.trim().isEmpty()) {
                        activeRide.destinationAddress = last.address;
                    }
                }
                bindRideData();
            }

            @Override
            public void onFailure(@NonNull Call<RideTrackingResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void fetchRideDetails() {
        rideApiService.getRideDetails(rideId).enqueue(new Callback<RideDetailsResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideDetailsResponse> call,
                @NonNull Response<RideDetailsResponse> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                bindRideDetails(response.body().ride);
                if (response.body().passengers != null && !response.body().passengers.isEmpty()) {
                    RideDetailsResponse.PassengerInfo firstPassenger = response.body().passengers.get(0);
                    if (firstPassenger != null) {
                        String fullName = joinName(firstPassenger.name, firstPassenger.surname);
                        String meta = firstPassenger.email == null || firstPassenger.email.trim().isEmpty()
                            ? context.getString(R.string.ride_tracking_stop_passenger_meta_unknown)
                            : firstPassenger.email;
                        bindPassengerInfo(fullName, meta);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideDetailsResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void bindRideDetails(RideDetailsResponse.RideInfo rideInfo) {
        if (rideInfo == null) {
            return;
        }
        if (rideInfo.totalDistanceKm != null) {
            activeRide.totalDistanceKm = rideInfo.totalDistanceKm;
            if (stopDistanceValueView != null) {
                stopDistanceValueView.setText(formatDistance(activeRide.totalDistanceKm));
            }
        }
        if (rideInfo.estimatedDurationSec != null && stopDurationValueView != null) {
            stopDurationValueView.setText(formatDuration(rideInfo.estimatedDurationSec));
        }

        List<RideDetailsResponse.WaypointInfo> waypoints = rideInfo.waypoints == null
            ? new ArrayList<>()
            : new ArrayList<>(rideInfo.waypoints);
        if (waypoints.isEmpty()) {
            return;
        }
        Collections.sort(waypoints, Comparator.comparingInt(wp -> wp.order == null ? 0 : wp.order));
        RideDetailsResponse.WaypointInfo first = waypoints.get(0);
        RideDetailsResponse.WaypointInfo last = waypoints.get(waypoints.size() - 1);
        if (first != null && stopPickupAddressView != null) {
            stopPickupAddressView.setText(safe(first.address, context.getString(R.string.ride_tracking_from_placeholder)));
        }
        if (last != null && stopDestinationAddressView != null) {
            stopDestinationAddressView.setText(safe(last.address, context.getString(R.string.ride_tracking_to_placeholder)));
        }
    }

    private void bindPassengerInfo(String fullName, String meta) {
        if (stopPassengerNameView != null) {
            stopPassengerNameView.setText(safe(fullName, context.getString(R.string.ride_tracking_stop_passenger_unknown)));
        }
        if (stopPassengerMetaView != null) {
            stopPassengerMetaView.setText(safe(meta, context.getString(R.string.ride_tracking_stop_passenger_meta_unknown)));
        }
        if (stopPassengerAvatarView != null) {
            stopPassengerAvatarView.setText(initials(fullName));
        }
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty() && last.isEmpty()) {
            return context.getString(R.string.ride_tracking_stop_passenger_unknown);
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return context.getString(R.string.ride_tracking_stop_passenger_initials_fallback);
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts.length > 0 && !parts[0].isEmpty() ? parts[0].substring(0, 1) : "";
        String second = parts.length > 1 && !parts[1].isEmpty() ? parts[1].substring(0, 1) : "";
        String initials = (first + second).toUpperCase(Locale.ROOT);
        return initials.isEmpty() ? context.getString(R.string.ride_tracking_stop_passenger_initials_fallback) : initials;
    }

    private void bindActions() {
        if (dialog == null) {
            return;
        }
        ImageView closeButton = dialog.findViewById(R.id.stopCloseButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }
        if (stopContinueButton != null) {
            stopContinueButton.setOnClickListener(v -> dialog.dismiss());
        }
        if (stopConfirmButton != null) {
            stopConfirmButton.setOnClickListener(v -> submitStopRide());
        }
    }

    private void submitStopRide() {
        if (submitting || stopAddressInput == null) {
            return;
        }
        String stopAddress = stopAddressInput.getText() == null
            ? ""
            : stopAddressInput.getText().toString().trim();
        if (stopAddress.isEmpty()) {
            showError(context.getString(R.string.ride_tracking_stop_address_required));
            return;
        }
        RoutePoint current = activeRide.currentLocation;
        if (current == null) {
            showError(context.getString(R.string.ride_tracking_stop_missing_location));
            return;
        }

        setSubmitting(true);
        RideStopRequest request = new RideStopRequest(stopAddress, current.lat, current.lng);
        rideApiService.stopRide(rideId, request).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(@NonNull Call<RideResponse> call, @NonNull Response<RideResponse> response) {
                setSubmitting(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showError(context.getString(R.string.ride_tracking_stop_failed_with_code, response.code()));
                    return;
                }
                callbackHandler.onRideStopped(response.body());
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideResponse> call, @NonNull Throwable t) {
                setSubmitting(false);
                showError(context.getString(R.string.ride_tracking_stop_failed));
            }
        });
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        if (stopConfirmButton != null) {
            stopConfirmButton.setEnabled(!value);
            stopConfirmButton.setText(value
                ? R.string.ride_tracking_stop_confirm_loading
                : R.string.ride_tracking_stop_confirm);
        }
        if (stopContinueButton != null) {
            stopContinueButton.setEnabled(!value);
        }
        if (!value) {
            clearError();
        }
    }

    private void showError(String message) {
        if (stopErrorText == null) {
            return;
        }
        stopErrorText.setText(message);
        stopErrorText.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        if (stopErrorText == null) {
            return;
        }
        stopErrorText.setText("");
        stopErrorText.setVisibility(View.GONE);
    }

    private void configureBottomSheet() {
        if (dialog == null) {
            return;
        }
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setDraggable(true);
        behavior.setSkipCollapsed(false);
        bottomSheet.post(() -> {
            if (dialog == null) {
                return;
            }
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            int maxHeight = (int) (screenHeight * 0.86f);
            int contentHeight = bottomSheet.getHeight();
            ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
            params.height = Math.min(contentHeight, maxHeight);
            bottomSheet.setLayoutParams(params);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    private String defaultStopAddress() {
        if (resolvedCurrentAddress != null && !resolvedCurrentAddress.trim().isEmpty()) {
            return resolvedCurrentAddress.trim();
        }
        return "";
    }

    private void prefillStopAddressIfEmpty() {
        if (stopAddressInput == null) {
            return;
        }
        String existing = stopAddressInput.getText() == null
            ? ""
            : stopAddressInput.getText().toString().trim();
        if (!existing.isEmpty()) {
            return;
        }
        String defaultValue = defaultStopAddress();
        if (!defaultValue.isEmpty()) {
            stopAddressInput.setText(defaultValue);
            stopAddressInput.setSelection(defaultValue.length());
            return;
        }
        maybeReverseGeocodeCurrentLocation();
    }

    private void maybeReverseGeocodeCurrentLocation() {
        if (stopAddressInput == null || geocodingService == null || isMapboxTokenMissing()) {
            return;
        }
        RoutePoint current = activeRide.currentLocation;
        if (current == null) {
            return;
        }
        String key = String.format(Locale.US, "%.5f,%.5f", current.lat, current.lng);
        if (reverseGeocodeInFlight || key.equals(lastReverseGeocodeKey)) {
            return;
        }
        reverseGeocodeInFlight = true;
        lastReverseGeocodeKey = key;
        geocodingService.reverseGeocodeAddress(
            String.valueOf(current.lng),
            String.valueOf(current.lat),
            1,
            "address,poi,place,locality,neighborhood",
            BuildConfig.MAPBOX_ACCESS_TOKEN
        ).enqueue(new Callback<MapboxGeocodingResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<MapboxGeocodingResponse> call,
                @NonNull Response<MapboxGeocodingResponse> response
            ) {
                reverseGeocodeInFlight = false;
                if (!response.isSuccessful() || response.body() == null || response.body().features == null || response.body().features.isEmpty()) {
                    return;
                }
                MapboxGeocodingResponse.Feature feature = response.body().features.get(0);
                if (feature == null || feature.place_name == null || feature.place_name.trim().isEmpty()) {
                    return;
                }
                resolvedCurrentAddress = feature.place_name.trim();
                String existing = stopAddressInput.getText() == null
                    ? ""
                    : stopAddressInput.getText().toString().trim();
                if (existing.isEmpty()) {
                    stopAddressInput.setText(resolvedCurrentAddress);
                    stopAddressInput.setSelection(resolvedCurrentAddress.length());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxGeocodingResponse> call, @NonNull Throwable t) {
                reverseGeocodeInFlight = false;
            }
        });
    }

    private boolean isMapboxTokenMissing() {
        String token = BuildConfig.MAPBOX_ACCESS_TOKEN;
        return token == null || token.trim().isEmpty() || "MAPBOX_API_KEY".equals(token);
    }

    private String formatDuration(Integer etaSeconds) {
        if (etaSeconds == null || etaSeconds <= 0) {
            return context.getString(R.string.ride_tracking_stop_duration_placeholder);
        }
        int minutes = Math.max(1, etaSeconds / 60);
        return context.getString(R.string.ride_tracking_stop_duration_value, minutes);
    }

    private String formatDistance(Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0) {
            return context.getString(R.string.ride_tracking_stop_distance_placeholder);
        }
        return context.getString(R.string.ride_tracking_stop_distance_value, distanceKm);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}
