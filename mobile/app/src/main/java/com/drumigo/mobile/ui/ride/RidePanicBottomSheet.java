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
        whatHappensSection = dialog.findViewById(R.id.panicWhatHappensSection);
        activeInfoSection = dialog.findViewById(R.id.panicActiveInfoSection);
    }

    private void bindRideInfo() {
        if (dialog == null) {
            return;
        }
        TextView driverValue = dialog.findViewById(R.id.panicDriverValue);
        TextView vehicleValue = dialog.findViewById(R.id.panicVehicleValue);
        TextView currentLocationValue = dialog.findViewById(R.id.panicCurrentLocationValue);
        if (driverValue != null) {
            driverValue.setText(buildDriverName());
        }
        if (vehicleValue != null) {
            vehicleValue.setText(buildVehicleInfo());
        }
        if (currentLocationValue != null) {
            currentLocationValue.setText(buildCurrentLocationInfo());
        }
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
        String first = safeTrim(ride.driverName);
        String last = safeTrim(ride.driverSurname);
        if (first.isEmpty() && last.isEmpty()) {
            return context.getString(R.string.ride_tracking_driver_placeholder);
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private String buildVehicleInfo() {
        if (ride == null) {
            return context.getString(R.string.ride_tracking_vehicle_placeholder);
        }
        String model = safeTrim(ride.vehicleModel);
        String plate = safeTrim(ride.vehicleLicensePlate);
        if (model.isEmpty() && plate.isEmpty()) {
            return context.getString(R.string.ride_tracking_vehicle_placeholder);
        }
        if (model.isEmpty()) {
            return plate;
        }
        if (plate.isEmpty()) {
            return model;
        }
        return model + " - " + plate;
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void cleanup() {
        holdHandler.removeCallbacks(holdProgressRunnable);
    }
}
