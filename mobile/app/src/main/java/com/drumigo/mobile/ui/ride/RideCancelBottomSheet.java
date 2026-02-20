package com.drumigo.mobile.ui.ride;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.ride.ActiveRide;
import com.drumigo.mobile.data.model.ride.RideCancelByDriverRequest;
import com.drumigo.mobile.data.model.ride.RideDetailsResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RideCancelBottomSheet {

    interface CallbackHandler {
        void onRideCancelled();
    }

    private static final int MIN_EXPLANATION_LENGTH = 10;

    private final Context context;
    private final RideApiService rideApiService;
    private final long rideId;
    private final ActiveRide activeRide;
    private final boolean driverMode;
    private final CallbackHandler callbackHandler;

    private BottomSheetDialog dialog;
    private TextView titleView;
    private TextView statusView;
    private TextView pickupAddressView;
    private TextView destinationAddressView;
    private TextView counterpartAvatarView;
    private TextView counterpartNameView;
    private TextView counterpartMetaView;
    private View driverSection;
    private View passengerSection;
    private TextView notAllowedView;
    private RadioGroup reasonGroup;
    private TextInputLayout explanationLayout;
    private TextInputEditText explanationInput;
    private TextView errorView;
    private MaterialButton keepButton;
    private MaterialButton confirmButton;

    private final Map<Integer, DriverCancelReason> reasonsByViewId = new HashMap<>();
    private DriverCancelReason selectedReason;
    private boolean submitting = false;

    private static final class DriverCancelReason {
        final String apiReasonType;
        final String label;
        final boolean requiresExplanation;

        DriverCancelReason(String apiReasonType, String label, boolean requiresExplanation) {
            this.apiReasonType = apiReasonType;
            this.label = label;
            this.requiresExplanation = requiresExplanation;
        }
    }

    private RideCancelBottomSheet(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @Nullable ActiveRide activeRide,
        boolean driverMode,
        @NonNull CallbackHandler callbackHandler
    ) {
        this.context = context;
        this.rideApiService = rideApiService;
        this.rideId = rideId;
        this.activeRide = activeRide;
        this.driverMode = driverMode;
        this.callbackHandler = callbackHandler;
    }

    static void show(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @Nullable ActiveRide activeRide,
        boolean driverMode,
        @NonNull CallbackHandler callbackHandler
    ) {
        new RideCancelBottomSheet(
            context,
            rideApiService,
            rideId,
            activeRide,
            driverMode,
            callbackHandler
        ).showInternal();
    }

    private void showInternal() {
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_cancel);
        dialog.setDismissWithAnimation(true);
        bindViews();
        bindReasonOptions();
        bindRideSummary();
        applyRoleUi();
        bindActions();
        fetchRideDetails();
        dialog.setOnShowListener(ignored -> configureBottomSheet());
        dialog.show();
    }

    private void bindViews() {
        if (dialog == null) {
            return;
        }
        titleView = dialog.findViewById(R.id.cancelSheetTitle);
        statusView = dialog.findViewById(R.id.cancelStatusText);
        pickupAddressView = dialog.findViewById(R.id.cancelPickupAddress);
        destinationAddressView = dialog.findViewById(R.id.cancelDestinationAddress);
        counterpartAvatarView = dialog.findViewById(R.id.cancelCounterpartyAvatar);
        counterpartNameView = dialog.findViewById(R.id.cancelCounterpartyName);
        counterpartMetaView = dialog.findViewById(R.id.cancelCounterpartyMeta);
        driverSection = dialog.findViewById(R.id.cancelDriverSection);
        passengerSection = dialog.findViewById(R.id.cancelPassengerSection);
        notAllowedView = dialog.findViewById(R.id.cancelNotAllowedText);
        reasonGroup = dialog.findViewById(R.id.cancelReasonGroup);
        explanationLayout = dialog.findViewById(R.id.cancelExplanationLayout);
        explanationInput = dialog.findViewById(R.id.cancelExplanationInput);
        errorView = dialog.findViewById(R.id.cancelErrorText);
        keepButton = dialog.findViewById(R.id.cancelKeepButton);
        confirmButton = dialog.findViewById(R.id.cancelConfirmButton);
    }

    private void bindReasonOptions() {
        if (dialog == null) {
            return;
        }
        registerReason(
            R.id.reasonPersonalEmergency,
            new DriverCancelReason(
                "DRIVER_PERSONAL_ISSUE",
                context.getString(R.string.ride_tracking_cancel_reason_personal_emergency),
                true
            )
        );
        registerReason(
            R.id.reasonVehicleIssue,
            new DriverCancelReason(
                "VEHICLE_ISSUE",
                context.getString(R.string.ride_tracking_cancel_reason_vehicle_issue),
                true
            )
        );
        registerReason(
            R.id.reasonUnsafePickup,
            new DriverCancelReason(
                "UNSAFE_PICK_UP_LOCATION",
                context.getString(R.string.ride_tracking_cancel_reason_unsafe_pickup),
                false
            )
        );
        registerReason(
            R.id.reasonPassengerAbsent,
            new DriverCancelReason(
                "PASSENGER_ABSENT",
                context.getString(R.string.ride_tracking_cancel_reason_passenger_absent),
                false
            )
        );
        registerReason(
            R.id.reasonOther,
            new DriverCancelReason(
                "OTHER",
                context.getString(R.string.ride_tracking_cancel_reason_other),
                true
            )
        );
    }

    private void registerReason(int viewId, DriverCancelReason reason) {
        reasonsByViewId.put(viewId, reason);
        if (dialog == null) {
            return;
        }
        RadioButton radio = dialog.findViewById(viewId);
        if (radio != null) {
            radio.setText(reason.label);
        }
    }

    private void bindRideSummary() {
        if (pickupAddressView != null) {
            pickupAddressView.setText(safe(
                activeRide == null ? null : activeRide.startAddress,
                context.getString(R.string.ride_tracking_from_placeholder)
            ));
        }
        if (destinationAddressView != null) {
            destinationAddressView.setText(safe(
                activeRide == null ? null : activeRide.destinationAddress,
                context.getString(R.string.ride_tracking_to_placeholder)
            ));
        }

        if (statusView != null) {
            statusView.setText(buildStatusText());
        }

        if (driverMode) {
            bindCounterparty(
                context.getString(R.string.ride_tracking_cancel_passenger_unknown),
                context.getString(R.string.ride_tracking_cancel_passenger_meta),
                context.getString(R.string.ride_tracking_cancel_passenger_initials)
            );
        } else {
            String driverName = joinName(
                activeRide == null ? null : activeRide.driverName,
                activeRide == null ? null : activeRide.driverSurname
            );
            String driverVehicle = joinVehicle(
                activeRide == null ? null : activeRide.vehicleModel,
                activeRide == null ? null : activeRide.vehicleLicensePlate
            );
            bindCounterparty(
                safe(driverName, context.getString(R.string.ride_tracking_driver_placeholder)),
                safe(driverVehicle, context.getString(R.string.ride_tracking_vehicle_placeholder)),
                initials(driverName)
            );
        }
    }

    private void applyRoleUi() {
        boolean canCancel = driverMode ? canDriverCancel() : canPassengerCancel();
        if (driverSection != null) {
            driverSection.setVisibility(driverMode ? View.VISIBLE : View.GONE);
        }
        if (passengerSection != null) {
            passengerSection.setVisibility(driverMode ? View.GONE : View.VISIBLE);
        }
        if (titleView != null) {
            titleView.setText(driverMode
                ? R.string.ride_tracking_cancel_sheet_title_driver
                : R.string.ride_tracking_cancel_sheet_title_passenger);
        }
        if (confirmButton != null) {
            confirmButton.setText(R.string.ride_tracking_cancel_confirm);
            confirmButton.setEnabled(canCancel);
        }
        if (notAllowedView != null) {
            if (canCancel) {
                notAllowedView.setVisibility(View.GONE);
            } else {
                notAllowedView.setVisibility(View.VISIBLE);
                notAllowedView.setText(driverMode
                    ? context.getString(R.string.ride_tracking_cancel_not_available_driver, normalizedStatus())
                    : context.getString(R.string.ride_tracking_cancel_not_available_passenger, normalizedStatus()));
            }
        }
        if (reasonGroup != null) {
            reasonGroup.setEnabled(canCancel);
            for (int i = 0; i < reasonGroup.getChildCount(); i++) {
                View child = reasonGroup.getChildAt(i);
                child.setEnabled(canCancel);
            }
        }
        if (passengerSection != null) {
            passengerSection.setEnabled(canCancel);
        }
    }

    private void bindActions() {
        if (dialog == null) {
            return;
        }
        ImageView closeButton = dialog.findViewById(R.id.cancelCloseButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }
        if (keepButton != null) {
            keepButton.setOnClickListener(v -> dialog.dismiss());
        }
        if (reasonGroup != null) {
            reasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
                selectedReason = reasonsByViewId.get(checkedId);
                updateExplanationVisibility();
                clearError();
            });
        }
        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> submitCancellation());
        }
    }

    private void updateExplanationVisibility() {
        if (explanationLayout == null || explanationInput == null) {
            return;
        }
        boolean show = driverMode && selectedReason != null && selectedReason.requiresExplanation;
        explanationLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            explanationInput.setText("");
            explanationLayout.setError(null);
        }
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
                bindDetails(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<RideDetailsResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void bindDetails(RideDetailsResponse details) {
        if (details == null) {
            return;
        }
        if (driverMode) {
            if (details.passengers != null && !details.passengers.isEmpty()) {
                RideDetailsResponse.PassengerInfo passenger = details.passengers.get(0);
                if (passenger != null) {
                    String fullName = joinName(passenger.name, passenger.surname);
                    bindCounterparty(
                        safe(fullName, context.getString(R.string.ride_tracking_cancel_passenger_unknown)),
                        safe(passenger.email, context.getString(R.string.ride_tracking_cancel_passenger_meta)),
                        initials(fullName)
                    );
                }
            }
            return;
        }

        String driverName = null;
        String driverMeta = null;
        if (details.driver != null) {
            driverName = joinName(details.driver.name, details.driver.surname);
            driverMeta = details.driver.phone;
        }
        if ((driverName == null || driverName.trim().isEmpty()) && details.ride != null) {
            driverName = joinName(details.ride.driverName, details.ride.driverSurname);
        }
        if (driverMeta == null || driverMeta.trim().isEmpty()) {
            driverMeta = joinVehicle(
                activeRide == null ? null : activeRide.vehicleModel,
                activeRide == null ? null : activeRide.vehicleLicensePlate
            );
        }
        bindCounterparty(
            safe(driverName, context.getString(R.string.ride_tracking_driver_placeholder)),
            safe(driverMeta, context.getString(R.string.ride_tracking_vehicle_placeholder)),
            initials(driverName)
        );
    }

    private void submitCancellation() {
        if (submitting) {
            return;
        }
        clearError();
        if (driverMode) {
            submitDriverCancellation();
        } else {
            submitPassengerCancellation();
        }
    }

    private void submitDriverCancellation() {
        if (!canDriverCancel()) {
            showError(context.getString(
                R.string.ride_tracking_cancel_not_available_driver,
                normalizedStatus()
            ));
            return;
        }
        if (selectedReason == null) {
            showError(context.getString(R.string.ride_tracking_cancel_reason_required));
            return;
        }
        String explanation = explanationInput == null || explanationInput.getText() == null
            ? ""
            : explanationInput.getText().toString().trim();
        if (selectedReason.requiresExplanation && explanation.length() < MIN_EXPLANATION_LENGTH) {
            if (explanationLayout != null) {
                explanationLayout.setError(context.getString(
                    R.string.ride_tracking_cancel_explanation_too_short,
                    MIN_EXPLANATION_LENGTH
                ));
            }
            showError(context.getString(
                R.string.ride_tracking_cancel_explanation_too_short,
                MIN_EXPLANATION_LENGTH
            ));
            return;
        }
        if (explanationLayout != null) {
            explanationLayout.setError(null);
        }
        String requestReason = selectedReason.requiresExplanation
            ? explanation
            : selectedReason.label;
        RideCancelByDriverRequest request = new RideCancelByDriverRequest(
            selectedReason.apiReasonType,
            requestReason
        );
        setSubmitting(true);
        rideApiService.cancelRideByDriver(rideId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setSubmitting(false);
                if (!response.isSuccessful()) {
                    showError(context.getString(
                        R.string.ride_tracking_cancel_failed_with_code,
                        response.code()
                    ));
                    return;
                }
                callbackHandler.onRideCancelled();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setSubmitting(false);
                showError(context.getString(R.string.ride_tracking_cancel_failed));
            }
        });
    }

    private void submitPassengerCancellation() {
        if (!canPassengerCancel()) {
            showError(context.getString(
                R.string.ride_tracking_cancel_not_available_passenger,
                normalizedStatus()
            ));
            return;
        }
        setSubmitting(true);
        rideApiService.cancelRideByPassenger(rideId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setSubmitting(false);
                if (!response.isSuccessful()) {
                    showError(context.getString(
                        R.string.ride_tracking_cancel_failed_with_code,
                        response.code()
                    ));
                    return;
                }
                callbackHandler.onRideCancelled();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setSubmitting(false);
                showError(context.getString(R.string.ride_tracking_cancel_failed));
            }
        });
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        if (confirmButton != null) {
            confirmButton.setEnabled(!value);
            confirmButton.setText(value
                ? R.string.ride_tracking_cancel_confirm_loading
                : R.string.ride_tracking_cancel_confirm);
        }
        if (keepButton != null) {
            keepButton.setEnabled(!value);
        }
    }

    private void showError(String message) {
        if (errorView == null) {
            return;
        }
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        if (errorView != null) {
            errorView.setText("");
            errorView.setVisibility(View.GONE);
        }
        if (explanationLayout != null) {
            explanationLayout.setError(null);
        }
    }

    private void bindCounterparty(String name, String meta, String avatarText) {
        if (counterpartNameView != null) {
            counterpartNameView.setText(name);
        }
        if (counterpartMetaView != null) {
            counterpartMetaView.setText(meta);
        }
        if (counterpartAvatarView != null) {
            counterpartAvatarView.setText(avatarText);
        }
    }

    private String buildStatusText() {
        String status = normalizedStatus();
        switch (status) {
            case "PENDING":
                return context.getString(R.string.ride_tracking_cancel_status_assigned);
            case "ACCEPTED":
            case "DRIVER_ARRIVING":
                return context.getString(R.string.ride_tracking_cancel_status_arriving);
            case "ACTIVE":
            case "IN_PROGRESS":
                return context.getString(R.string.ride_tracking_cancel_status_in_progress);
            case "SCHEDULED":
                return context.getString(R.string.ride_tracking_cancel_status_scheduled);
            case "CANCELLED":
                return context.getString(R.string.ride_tracking_cancel_status_cancelled);
            case "FINISHED":
            case "COMPLETED":
                return context.getString(R.string.ride_tracking_cancel_status_completed);
            default:
                return context.getString(R.string.ride_tracking_cancel_status_unknown, status);
        }
    }

    private boolean canDriverCancel() {
        String status = normalizedStatus();
        return "PENDING".equals(status)
            || "ACCEPTED".equals(status)
            || "DRIVER_ARRIVING".equals(status);
    }

    private boolean canPassengerCancel() {
        String status = normalizedStatus();
        return "PENDING".equals(status)
            || "ACCEPTED".equals(status);
    }

    private String normalizedStatus() {
        if (activeRide == null || activeRide.status == null) {
            return "UNKNOWN";
        }
        return activeRide.status.trim().toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
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
        String safeModel = model == null ? "" : model.trim();
        String safePlate = plate == null ? "" : plate.trim();
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

    private String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts.length > 0 && !parts[0].isEmpty() ? parts[0].substring(0, 1) : "";
        String second = parts.length > 1 && !parts[1].isEmpty() ? parts[1].substring(0, 1) : "";
        String initials = (first + second).toUpperCase(Locale.ROOT);
        return initials.isEmpty() ? "?" : initials;
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
            int maxHeight = (int) (screenHeight * 0.84f);
            int contentHeight = bottomSheet.getHeight();
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = Math.min(contentHeight, maxHeight);
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }
}
