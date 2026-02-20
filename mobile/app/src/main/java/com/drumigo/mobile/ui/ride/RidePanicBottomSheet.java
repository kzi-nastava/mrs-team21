package com.drumigo.mobile.ui.ride;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.ride.ActiveRide;
import com.drumigo.mobile.data.model.ride.RideDetailsResponse;
import com.drumigo.mobile.data.model.ride.RideTrackingResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RidePanicBottomSheet {

    interface CallbackHandler {
        void onPanicSent();
    }

    private static final long HOLD_DURATION_MS = 2000L;
    private static final long HOLD_TICK_MS = 40L;

    private final Context context;
    private final RideApiService rideApiService;
    private final long rideId;
    private final ActiveRide ride;
    private final CallbackHandler callbackHandler;

    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private BottomSheetDialog dialog;

    private MaterialButton holdButton;
    private LinearProgressIndicator holdProgress;
    private TextView instructionText;
    private TextView statusText;
    private TextView driverValueView;
    private TextView vehicleValueView;
    private TextView currentLocationValueView;
    private View whatHappensSection;
    private View activeInfoSection;

    private boolean panicSent;
    private boolean requestInFlight;
    private boolean holdActive;
    private long holdStartedAt;

    private final Runnable holdProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!holdActive || holdProgress == null) {
                return;
            }
            long elapsed = SystemClock.uptimeMillis() - holdStartedAt;
            int progress = (int) Math.min(100L, (elapsed * 100L) / HOLD_DURATION_MS);
            holdProgress.setProgressCompat(progress, true);

            if (elapsed >= HOLD_DURATION_MS) {
                holdActive = false;
                triggerPanicRequest();
                return;
            }
            holdHandler.postDelayed(this, HOLD_TICK_MS);
        }
    };

    private RidePanicBottomSheet(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @Nullable ActiveRide ride,
        boolean panicAlreadySent,
        @NonNull CallbackHandler callbackHandler
    ) {
        this.context = context;
        this.rideApiService = rideApiService;
        this.rideId = rideId;
        this.ride = ride;
        this.panicSent = panicAlreadySent;
        this.callbackHandler = callbackHandler;
    }

    static void show(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @Nullable ActiveRide ride,
        boolean panicAlreadySent,
        @NonNull CallbackHandler callbackHandler
    ) {
        new RidePanicBottomSheet(
            context,
            rideApiService,
            rideId,
            ride,
            panicAlreadySent,
            callbackHandler
        ).showInternal();
    }

    private void showInternal() {
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_panic);
        dialog.setDismissWithAnimation(true);

        bindViews();
        bindRideInfo();
        fetchLiveRideInfo();
        bindActions();
        applyPanicStateUi();

        dialog.setOnShowListener(ignored -> configureBottomSheet());
        dialog.setOnDismissListener(ignored -> cleanup());
        dialog.show();
    }

    private void bindViews() {
        if (dialog == null) {
            return;
        }
        holdButton = dialog.findViewById(R.id.panicHoldButton);
        holdProgress = dialog.findViewById(R.id.panicHoldProgress);
        instructionText = dialog.findViewById(R.id.panicInstructionText);
        statusText = dialog.findViewById(R.id.panicStatusText);
        driverValueView = dialog.findViewById(R.id.panicDriverValue);
        vehicleValueView = dialog.findViewById(R.id.panicVehicleValue);
        currentLocationValueView = dialog.findViewById(R.id.panicCurrentLocationValue);
        whatHappensSection = dialog.findViewById(R.id.panicWhatHappensSection);
        activeInfoSection = dialog.findViewById(R.id.panicActiveInfoSection);
    }

    private void bindRideInfo() {
        if (dialog == null) {
            return;
        }
        if (driverValueView != null) {
            driverValueView.setText(buildDriverName());
        }
        if (vehicleValueView != null) {
            vehicleValueView.setText(buildVehicleInfo());
        }
        if (currentLocationValueView != null) {
            currentLocationValueView.setText(buildCurrentLocationInfo());
        }
    }

    private void fetchLiveRideInfo() {
        fetchTrackingInfo();
        fetchRideDetailsInfo();
    }

    private void fetchTrackingInfo() {
        rideApiService.getRideTracking(rideId).enqueue(new Callback<RideTrackingResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideTrackingResponse> call,
                @NonNull Response<RideTrackingResponse> response
            ) {
                if (dialog == null || !response.isSuccessful() || response.body() == null) {
                    return;
                }
                RideTrackingResponse tracking = response.body();

                String trackingDriver = joinName(tracking.driverName, tracking.driverSurname);
                if (driverValueView != null && !trackingDriver.isEmpty()) {
                    driverValueView.setText(trackingDriver);
                }

                String trackingVehicle = joinVehicle(tracking.vehicleModel, tracking.vehicleLicensePlate);
                if (vehicleValueView != null && !trackingVehicle.isEmpty()) {
                    vehicleValueView.setText(trackingVehicle);
                }

                String trackingLocation = formatLocation(tracking.vehicleCurrentLat, tracking.vehicleCurrentLng);
                if (trackingLocation.isEmpty()) {
                    trackingLocation = resolveLocationFromWaypoints(tracking);
                }
                if (currentLocationValueView != null && !trackingLocation.isEmpty()) {
                    currentLocationValueView.setText(trackingLocation);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideTrackingResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void fetchRideDetailsInfo() {
        rideApiService.getRideDetails(rideId).enqueue(new Callback<RideDetailsResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideDetailsResponse> call,
                @NonNull Response<RideDetailsResponse> response
            ) {
                if (dialog == null || !response.isSuccessful() || response.body() == null) {
                    return;
                }
                RideDetailsResponse body = response.body();
                if (body.ride != null) {
                    String rideDriver = joinName(body.ride.driverName, body.ride.driverSurname);
                    if (driverValueView != null && !rideDriver.isEmpty()) {
                        driverValueView.setText(rideDriver);
                    }
                    String waypointLocation = resolveLocationFromWaypoints(body.ride.waypoints);
                    if (currentLocationValueView != null && !waypointLocation.isEmpty()) {
                        currentLocationValueView.setText(waypointLocation);
                    }
                }
                if (body.driver != null) {
                    String detailsDriver = joinName(body.driver.name, body.driver.surname);
                    if (driverValueView != null && !detailsDriver.isEmpty()) {
                        driverValueView.setText(detailsDriver);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideDetailsResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void bindActions() {
        if (dialog == null) {
            return;
        }
        ImageView closeButton = dialog.findViewById(R.id.panicCloseButton);

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }
        if (holdButton != null) {
            holdButton.setOnClickListener(v -> {
                // Hold interaction handled via touch events.
            });
            holdButton.setOnTouchListener((v, event) -> {
                if (panicSent || requestInFlight) {
                    return false;
                }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startHolding();
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        cancelHolding();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE:
                        cancelHolding();
                        return true;
                    default:
                        return false;
                }
            });
        }
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
            int maxHeight = (int) (screenHeight * 0.78f);
            int contentHeight = bottomSheet.getHeight();
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = Math.min(contentHeight, maxHeight);
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    private void startHolding() {
        if (panicSent || requestInFlight || holdActive) {
            return;
        }
        holdActive = true;
        holdStartedAt = SystemClock.uptimeMillis();
        if (instructionText != null) {
            instructionText.setText(R.string.ride_tracking_panic_hold_in_progress);
        }
        if (holdProgress != null) {
            holdProgress.setVisibility(View.VISIBLE);
            holdProgress.setIndeterminate(false);
            holdProgress.setProgressCompat(0, false);
        }
        holdHandler.removeCallbacks(holdProgressRunnable);
        holdHandler.post(holdProgressRunnable);
    }

    private void cancelHolding() {
        if (!holdActive) {
            return;
        }
        holdActive = false;
        holdHandler.removeCallbacks(holdProgressRunnable);
        if (panicSent || requestInFlight) {
            return;
        }
        if (holdProgress != null) {
            holdProgress.setProgressCompat(0, false);
            holdProgress.setVisibility(View.INVISIBLE);
        }
        if (instructionText != null) {
            instructionText.setText(R.string.ride_tracking_panic_sheet_subtitle);
        }
    }

    private void triggerPanicRequest() {
        holdHandler.removeCallbacks(holdProgressRunnable);
        requestInFlight = true;
        if (holdProgress != null) {
            holdProgress.setVisibility(View.VISIBLE);
            holdProgress.setIndeterminate(true);
        }
        if (instructionText != null) {
            instructionText.setText(R.string.ride_tracking_panic_sending);
        }
        if (holdButton != null) {
            holdButton.setEnabled(false);
        }

        rideApiService.createPanic(rideId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                requestInFlight = false;
                if (response.isSuccessful()) {
                    panicSent = true;
                    applyPanicStateUi();
                    callbackHandler.onPanicSent();
                    return;
                }
                resetHoldUiAfterFailure();
                if (instructionText != null) {
                    instructionText.setText(context.getString(
                        R.string.ride_tracking_panic_send_failed_with_code,
                        response.code()
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                requestInFlight = false;
                resetHoldUiAfterFailure();
                if (instructionText != null) {
                    instructionText.setText(R.string.ride_tracking_panic_send_failed);
                }
            }
        });
    }

    private void resetHoldUiAfterFailure() {
        if (holdButton != null) {
            holdButton.setEnabled(true);
        }
        if (holdProgress != null) {
            holdProgress.setIndeterminate(false);
            holdProgress.setProgressCompat(0, false);
            holdProgress.setVisibility(View.INVISIBLE);
        }
        if (statusText != null) {
            statusText.setText(R.string.ride_tracking_panic_status_ready);
            statusText.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
            statusText.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.bg_cream)
            ));
        }
    }

    private void applyPanicStateUi() {
        if (holdButton == null || holdProgress == null || instructionText == null || statusText == null) {
            return;
        }
        if (panicSent) {
            holdButton.setEnabled(false);
            holdButton.setText(R.string.ride_tracking_panic_help_sent);
            holdButton.setIconResource(R.drawable.ic_check);
            holdButton.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.success)
            ));
            holdProgress.setIndeterminate(false);
            holdProgress.setVisibility(View.INVISIBLE);
            holdProgress.setProgressCompat(100, false);
            instructionText.setText(R.string.ride_tracking_panic_help_on_way);
            statusText.setText(R.string.ride_tracking_panic_status_sent);
            statusText.setTextColor(ContextCompat.getColor(context, R.color.error));
            statusText.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.accent_08)
            ));
            if (whatHappensSection != null) {
                whatHappensSection.setVisibility(View.GONE);
            }
            if (activeInfoSection != null) {
                activeInfoSection.setVisibility(View.VISIBLE);
            }
            return;
        }

        holdButton.setEnabled(true);
        holdButton.setText(R.string.ride_tracking_panic_hold_label);
        holdButton.setIconResource(R.drawable.ic_warning);
        holdButton.setBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.error)
        ));
        holdProgress.setIndeterminate(false);
        holdProgress.setProgressCompat(0, false);
        holdProgress.setVisibility(View.INVISIBLE);
        instructionText.setText(R.string.ride_tracking_panic_sheet_subtitle);
        statusText.setText(R.string.ride_tracking_panic_status_ready);
        statusText.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
        statusText.setBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.bg_cream)
        ));
        if (whatHappensSection != null) {
            whatHappensSection.setVisibility(View.VISIBLE);
        }
        if (activeInfoSection != null) {
            activeInfoSection.setVisibility(View.GONE);
        }
    }

    private String buildDriverName() {
        if (ride == null) {
            return context.getString(R.string.ride_tracking_driver_placeholder);
        }
        String fullName = joinName(ride.driverName, ride.driverSurname);
        if (fullName.isEmpty()) {
            return context.getString(R.string.ride_tracking_driver_placeholder);
        }
        return fullName;
    }

    private String buildVehicleInfo() {
        if (ride == null) {
            return context.getString(R.string.ride_tracking_vehicle_placeholder);
        }
        String vehicle = joinVehicle(ride.vehicleModel, ride.vehicleLicensePlate);
        if (vehicle.isEmpty()) {
            return context.getString(R.string.ride_tracking_vehicle_placeholder);
        }
        return vehicle;
    }

    private String buildCurrentLocationInfo() {
        if (ride == null) {
            return context.getString(R.string.ride_tracking_panic_current_location_unknown);
        }
        if (ride.currentLocation != null) {
            return String.format(
                Locale.US,
                "%.5f, %.5f",
                ride.currentLocation.lat,
                ride.currentLocation.lng
            );
        }
        String destination = safeTrim(ride.destinationAddress);
        if (!destination.isEmpty()) {
            return destination;
        }
        String start = safeTrim(ride.startAddress);
        if (!start.isEmpty()) {
            return start;
        }
        return context.getString(R.string.ride_tracking_panic_current_location_unknown);
    }

    private String joinName(String firstName, String lastName) {
        String first = safeTrim(firstName);
        String last = safeTrim(lastName);
        if (first.isEmpty() && last.isEmpty()) {
            return "";
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private String joinVehicle(String model, String plate) {
        String safeModel = safeTrim(model);
        String safePlate = safeTrim(plate);
        if (safeModel.isEmpty() && safePlate.isEmpty()) {
            return "";
        }
        if (safeModel.isEmpty()) {
            return safePlate;
        }
        if (safePlate.isEmpty()) {
            return safeModel;
        }
        return safeModel + " - " + safePlate;
    }

    private String formatLocation(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return "";
        }
        return String.format(Locale.US, "%.5f, %.5f", lat, lng);
    }

    private String resolveLocationFromWaypoints(RideTrackingResponse tracking) {
        if (tracking == null || tracking.waypoints == null || tracking.waypoints.isEmpty()) {
            return "";
        }
        java.util.List<RideTrackingResponse.WaypointInfo> sorted = new java.util.ArrayList<>(tracking.waypoints);
        java.util.Collections.sort(sorted, (a, b) -> {
            int orderA = a == null || a.order == null ? 0 : a.order;
            int orderB = b == null || b.order == null ? 0 : b.order;
            return Integer.compare(orderA, orderB);
        });
        RideTrackingResponse.WaypointInfo destination = sorted.get(sorted.size() - 1);
        if (destination != null && destination.address != null && !destination.address.trim().isEmpty()) {
            return destination.address.trim();
        }
        return "";
    }

    private String resolveLocationFromWaypoints(java.util.List<RideDetailsResponse.WaypointInfo> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return "";
        }
        java.util.List<RideDetailsResponse.WaypointInfo> sorted = new java.util.ArrayList<>(waypoints);
        java.util.Collections.sort(sorted, (a, b) -> {
            int orderA = a == null || a.order == null ? 0 : a.order;
            int orderB = b == null || b.order == null ? 0 : b.order;
            return Integer.compare(orderA, orderB);
        });
        RideDetailsResponse.WaypointInfo destination = sorted.get(sorted.size() - 1);
        if (destination == null) {
            return "";
        }
        String destinationAddress = safeTrim(destination.address);
        if (!destinationAddress.isEmpty()) {
            return destinationAddress;
        }
        return formatLocation(destination.lat, destination.lng);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void cleanup() {
        holdHandler.removeCallbacks(holdProgressRunnable);
    }
}
