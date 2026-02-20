package com.drumigo.mobile.ui.history;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.Ride;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

final class RideHistoryDetailsBottomSheet {

    private RideHistoryDetailsBottomSheet() {
    }

    static void show(@NonNull Context context, @NonNull Ride ride) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_history_details);
        dialog.setDismissWithAnimation(true);

        bindRide(dialog, context, ride);

        dialog.setOnShowListener(ignored -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) {
                return;
            }
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setDraggable(true);
            behavior.setSkipCollapsed(false);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        dialog.show();
    }

    private static void bindRide(BottomSheetDialog dialog, Context context, Ride ride) {
        TextView rideIdText = dialog.findViewById(R.id.detailRideIdText);
        TextView statusText = dialog.findViewById(R.id.detailStatusText);
        TextView dateText = dialog.findViewById(R.id.detailDateText);
        TextView timeText = dialog.findViewById(R.id.detailTimeText);
        TextView passengerCountText = dialog.findViewById(R.id.detailPassengerCountText);
        TextView amountText = dialog.findViewById(R.id.detailAmountText);
        TextView cancellationText = dialog.findViewById(R.id.detailCancellationText);
        TextView panicText = dialog.findViewById(R.id.detailPanicText);
        TextView originText = dialog.findViewById(R.id.detailOriginText);
        TextView destinationText = dialog.findViewById(R.id.detailDestinationText);
        ImageView closeButton = dialog.findViewById(R.id.rideDetailsCloseButton);
        ImageView routeMapButton = dialog.findViewById(R.id.detailRouteMapButton);

        if (rideIdText != null) {
            long safeId = ride.getId() == null ? 0L : ride.getId();
            rideIdText.setText(context.getString(R.string.ride_history_detail_ride_id, safeId));
        }
        if (statusText != null) {
            RideStatusPresentation status = RideStatusPresentation.from(ride.getStatus());
            statusText.setText(status.label);
            statusText.setBackgroundResource(status.badgeBackgroundRes);
            statusText.setTextColor(ContextCompat.getColor(context, status.textColorRes));
        }
        if (dateText != null) {
            dateText.setText(isBlank(ride.getDate()) ? context.getString(R.string.ride_history_none) : ride.getDate());
        }
        if (timeText != null) {
            timeText.setText(isBlank(ride.getTime()) ? context.getString(R.string.ride_history_none) : ride.getTime());
        }
        if (passengerCountText != null) {
            passengerCountText.setText(String.valueOf(Math.max(0, ride.getPassengerCount())));
        }
        if (amountText != null) {
            String amount = isBlank(ride.getPrice()) ? context.getString(R.string.ride_history_none) : ride.getPrice();
            amountText.setText(amount);
            int amountColor = resolveAmountColorRes(ride.getPrice());
            amountText.setTextColor(ContextCompat.getColor(context, amountColor));
        }
        if (cancellationText != null) {
            cancellationText.setText(
                isBlank(ride.getCancelledBy())
                    ? context.getString(R.string.ride_history_not_cancelled)
                    : ride.getCancelledBy()
            );
        }
        if (panicText != null) {
            panicText.setText(ride.hasPanic()
                ? context.getString(R.string.ride_history_panic_active)
                : context.getString(R.string.ride_history_panic_inactive));
            panicText.setTextColor(ContextCompat.getColor(context, ride.hasPanic() ? R.color.danger : R.color.text_dark));
        }
        if (originText != null) {
            originText.setText(isBlank(ride.getOrigin()) ? context.getString(R.string.ride_history_none) : ride.getOrigin());
        }
        if (destinationText != null) {
            destinationText.setText(
                isBlank(ride.getDestination()) ? context.getString(R.string.ride_history_none) : ride.getDestination()
            );
        }

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }
        if (routeMapButton != null) {
            routeMapButton.setOnClickListener(v -> RideHistoryRouteMapDialog.show(context, ride));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int resolveAmountColorRes(String value) {
        if (isBlank(value)) {
            return R.color.text_light;
        }
        return value.trim().startsWith("+") ? R.color.success : R.color.primary_dark;
    }
}
