package com.drumigo.mobile.ui.history;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.ride.RideRatingStatusResponse;
import com.drumigo.mobile.data.model.ride.ReviewResponse;
import com.drumigo.mobile.session.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RideHistoryDetailsBottomSheet {

    private RideHistoryDetailsBottomSheet() {
    }

    static void show(
        @NonNull Context context,
        @NonNull Ride ride,
        boolean showRatingSection,
        RideApiService rideApiService
    ) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_history_details);
        dialog.setDismissWithAnimation(true);

        bindRide(dialog, context, ride);
        boolean isCompleted = ride.getStatus() != null
            && ride.getStatus().trim().equalsIgnoreCase("Completed");
        if (showRatingSection && rideApiService != null && ride.getId() != null && ride.getId() > 0 && isCompleted) {
            bindRatingSection(dialog, context, ride, rideApiService);
        }

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

    private static void bindRatingSection(
        BottomSheetDialog dialog,
        Context context,
        Ride ride,
        RideApiService rideApiService
    ) {
        View container = dialog.findViewById(R.id.ratingSectionContainer);
        if (container == null) return;
        container.setVisibility(View.VISIBLE);

        TextView loadingText = dialog.findViewById(R.id.ratingLoadingText);
        LinearLayout canRateContainer = dialog.findViewById(R.id.ratingCanRateContainer);
        LinearLayout submittedContainer = dialog.findViewById(R.id.ratingSubmittedContainer);
        TextView expiredText = dialog.findViewById(R.id.ratingExpiredText);
        MaterialButton actionButton = dialog.findViewById(R.id.ratingActionButton);

        long rideId = ride.getId();
        Runnable refreshRating = () -> loadRatingStatus(dialog, context, ride, rideApiService);

        if (loadingText != null) loadingText.setVisibility(View.VISIBLE);
        if (canRateContainer != null) canRateContainer.setVisibility(View.GONE);
        if (submittedContainer != null) submittedContainer.setVisibility(View.GONE);
        if (expiredText != null) expiredText.setVisibility(View.GONE);

        rideApiService.getRatingStatus(rideId).enqueue(new Callback<RideRatingStatusResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideRatingStatusResponse> call,
                @NonNull Response<RideRatingStatusResponse> response
            ) {
                if (loadingText != null) loadingText.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) return;
                RideRatingStatusResponse status = response.body();
                applyRatingStatus(
                    dialog, context, rideId, status, rideApiService,
                    canRateContainer, submittedContainer, expiredText,
                    actionButton, ride, refreshRating
                );
            }

            @Override
            public void onFailure(@NonNull Call<RideRatingStatusResponse> call, @NonNull Throwable t) {
                if (loadingText != null) loadingText.setVisibility(View.GONE);
            }
        });
    }

    private static void loadRatingStatus(
        BottomSheetDialog dialog,
        Context context,
        Ride ride,
        RideApiService rideApiService
    ) {
        View container = dialog.findViewById(R.id.ratingSectionContainer);
        if (container == null || container.getVisibility() != View.VISIBLE) return;

        TextView loadingText = dialog.findViewById(R.id.ratingLoadingText);
        LinearLayout canRateContainer = dialog.findViewById(R.id.ratingCanRateContainer);
        LinearLayout submittedContainer = dialog.findViewById(R.id.ratingSubmittedContainer);
        TextView expiredText = dialog.findViewById(R.id.ratingExpiredText);
        MaterialButton actionButton = dialog.findViewById(R.id.ratingActionButton);

        long rideId = ride.getId();
        Runnable refreshRating = () -> loadRatingStatus(dialog, context, ride, rideApiService);

        if (loadingText != null) loadingText.setVisibility(View.VISIBLE);
        if (canRateContainer != null) canRateContainer.setVisibility(View.GONE);
        if (submittedContainer != null) submittedContainer.setVisibility(View.GONE);
        if (expiredText != null) expiredText.setVisibility(View.GONE);

        rideApiService.getRatingStatus(rideId).enqueue(new Callback<RideRatingStatusResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<RideRatingStatusResponse> call,
                @NonNull Response<RideRatingStatusResponse> response
            ) {
                if (loadingText != null) loadingText.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) return;
                RideRatingStatusResponse status = response.body();
                applyRatingStatus(
                    dialog, context, rideId, status, rideApiService,
                    canRateContainer, submittedContainer, expiredText,
                    actionButton, ride, refreshRating
                );
            }

            @Override
            public void onFailure(@NonNull Call<RideRatingStatusResponse> call, @NonNull Throwable t) {
                if (loadingText != null) loadingText.setVisibility(View.GONE);
            }
        });
    }

    private static void applyRatingStatus(
        BottomSheetDialog dialog,
        Context context,
        long rideId,
        RideRatingStatusResponse status,
        RideApiService rideApiService,
        LinearLayout canRateContainer,
        LinearLayout submittedContainer,
        TextView expiredText,
        MaterialButton actionButton,
        Ride ride,
        Runnable refreshRating
    ) {
        if (status.canRate) {
            if (canRateContainer != null) {
                canRateContainer.setVisibility(View.VISIBLE);
                TextView daysLeft = canRateContainer.findViewById(R.id.ratingDaysLeftText);
                if (daysLeft != null) {
                    if (status.daysRemaining <= 0) {
                        daysLeft.setText(R.string.ride_history_rating_day_left);
                    } else {
                        daysLeft.setText(context.getString(R.string.ride_history_rating_days_left, status.daysRemaining));
                    }
                }
            }
            if (actionButton != null) {
                actionButton.setOnClickListener(v -> {
                    RideRatingBottomSheet.show(context, rideApiService, rideId, ride, () -> refreshRating.run());
                });
            }
            if (submittedContainer != null) submittedContainer.setVisibility(View.GONE);
            if (expiredText != null) expiredText.setVisibility(View.GONE);
        } else if (status.hasReview) {
            if (canRateContainer != null) canRateContainer.setVisibility(View.GONE);
            if (submittedContainer != null) submittedContainer.setVisibility(View.VISIBLE);
            if (expiredText != null) expiredText.setVisibility(View.GONE);
            fetchAndShowReview(dialog, context, rideId, rideApiService);
        } else {
            if (canRateContainer != null) canRateContainer.setVisibility(View.GONE);
            if (submittedContainer != null) submittedContainer.setVisibility(View.GONE);
            if (expiredText != null) expiredText.setVisibility(View.VISIBLE);
        }
    }

    private static void fetchAndShowReview(
        BottomSheetDialog dialog,
        Context context,
        long rideId,
        RideApiService rideApiService
    ) {
        rideApiService.getRideReviews(rideId).enqueue(new Callback<List<ReviewResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<List<ReviewResponse>> call,
                @NonNull Response<List<ReviewResponse>> response
            ) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<ReviewResponse> reviews = response.body();
                long currentUserId = -1;
                try {
                    SessionManager sm = SessionManager.getInstance(context);
                    if (sm != null && sm.isAuthenticated()) {
                        currentUserId = sm.getUserId();
                    }
                } catch (Exception ignored) { }
                ReviewResponse myReview = null;
                for (ReviewResponse r : reviews) {
                    if (r.passengerId != null && r.passengerId == currentUserId) {
                        myReview = r;
                        break;
                    }
                }
                if (myReview == null && !reviews.isEmpty()) {
                    myReview = reviews.get(0);
                }
                if (myReview != null) {
                    TextView driverValue = dialog.findViewById(R.id.ratingDriverValue);
                    TextView vehicleValue = dialog.findViewById(R.id.ratingVehicleValue);
                    TextView commentText = dialog.findViewById(R.id.ratingCommentText);
                    View valuesRow = dialog.findViewById(R.id.ratingSubmittedValues);
                    if (driverValue != null && myReview.ratingDriver != null) {
                        driverValue.setText(myReview.ratingDriver + "/5");
                        driverValue.setVisibility(View.VISIBLE);
                    }
                    if (vehicleValue != null && myReview.ratingVehicle != null) {
                        vehicleValue.setText(myReview.ratingVehicle + "/5");
                        vehicleValue.setVisibility(View.VISIBLE);
                    }
                    if (valuesRow != null) valuesRow.setVisibility(View.VISIBLE);
                    if (commentText != null && myReview.comment != null && !myReview.comment.trim().isEmpty()) {
                        commentText.setText("\"" + myReview.comment.trim() + "\"");
                        commentText.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReviewResponse>> call, @NonNull Throwable t) { }
        });
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
