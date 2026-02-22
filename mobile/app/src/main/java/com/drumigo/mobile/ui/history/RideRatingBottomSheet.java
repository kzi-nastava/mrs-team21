package com.drumigo.mobile.ui.history;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.ride.ReviewCreateRequest;
import com.drumigo.mobile.data.model.ride.ReviewResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RideRatingBottomSheet {

    private static final int MAX_COMMENT_LENGTH = 500;

    interface CallbackHandler {
        void onReviewSubmitted();
    }

    private final Context context;
    private final RideApiService rideApiService;
    private final long rideId;
    private final Ride ride;
    private final CallbackHandler callbackHandler;

    private BottomSheetDialog dialog;
    private TextView originText;
    private TextView destinationText;
    private ImageView[] driverStars;
    private ImageView[] vehicleStars;
    private TextInputEditText commentInput;
    private TextView charCountText;
    private TextView errorText;
    private MaterialButton submitButton;

    private int driverRating = 0;
    private int vehicleRating = 0;
    private boolean submitting;

    private RideRatingBottomSheet(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @NonNull Ride ride,
        CallbackHandler callbackHandler
    ) {
        this.context = context;
        this.rideApiService = rideApiService;
        this.rideId = rideId;
        this.ride = ride;
        this.callbackHandler = callbackHandler;
    }

    static void show(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @NonNull Ride ride,
        CallbackHandler callbackHandler
    ) {
        new RideRatingBottomSheet(context, rideApiService, rideId, ride, callbackHandler).showInternal();
    }

    private void showInternal() {
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_rating);
        dialog.setDismissWithAnimation(true);

        originText = dialog.findViewById(R.id.rideRatingOriginText);
        destinationText = dialog.findViewById(R.id.rideRatingDestinationText);
        driverStars = new ImageView[] {
            dialog.findViewById(R.id.rideRatingDriverStar1),
            dialog.findViewById(R.id.rideRatingDriverStar2),
            dialog.findViewById(R.id.rideRatingDriverStar3),
            dialog.findViewById(R.id.rideRatingDriverStar4),
            dialog.findViewById(R.id.rideRatingDriverStar5),
        };
        vehicleStars = new ImageView[] {
            dialog.findViewById(R.id.rideRatingVehicleStar1),
            dialog.findViewById(R.id.rideRatingVehicleStar2),
            dialog.findViewById(R.id.rideRatingVehicleStar3),
            dialog.findViewById(R.id.rideRatingVehicleStar4),
            dialog.findViewById(R.id.rideRatingVehicleStar5),
        };
        commentInput = dialog.findViewById(R.id.rideRatingCommentInput);
        charCountText = dialog.findViewById(R.id.rideRatingCharCount);
        errorText = dialog.findViewById(R.id.rideRatingErrorText);
        submitButton = dialog.findViewById(R.id.rideRatingSubmitButton);
        View closeButton = dialog.findViewById(R.id.rideRatingCloseButton);
        MaterialButton skipButton = dialog.findViewById(R.id.rideRatingSkipButton);

        if (originText != null) {
            originText.setText(ride.getOrigin() != null ? ride.getOrigin() : "");
        }
        if (destinationText != null) {
            destinationText.setText(ride.getDestination() != null ? ride.getDestination() : "");
        }

        setupStarRow(driverStars, value -> {
            driverRating = value;
            updateStarAppearance(driverStars, driverRating);
        });
        setupStarRow(vehicleStars, value -> {
            vehicleRating = value;
            updateStarAppearance(vehicleStars, vehicleRating);
        });

        if (commentInput != null) {
            commentInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updateCharCount();
                }
            });
        }
        updateCharCount();

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }
        if (skipButton != null) {
            skipButton.setOnClickListener(v -> dismiss());
        }
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> onSubmit());
        }

        dialog.setOnShowListener(ignored -> {
            if (dialog != null) {
                View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
        dialog.show();
    }

    private void setupStarRow(ImageView[] stars, OnStarSelectedListener listener) {
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> listener.onStarSelected(rating));
        }
    }

    private interface OnStarSelectedListener {
        void onStarSelected(int rating);
    }

    private void updateStarAppearance(ImageView[] stars, int selectedRating) {
        int filledColor = ContextCompat.getColor(context, R.color.star_filled);
        int outlineColor = ContextCompat.getColor(context, R.color.text_light);
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            boolean filled = (i + 1) <= selectedRating;
            stars[i].setImageResource(filled ? R.drawable.ic_star : R.drawable.ic_star_outline);
            stars[i].setColorFilter(filled ? filledColor : outlineColor);
        }
    }

    private void updateCharCount() {
        if (charCountText == null || commentInput == null) return;
        int len = commentInput.getText() != null ? commentInput.getText().length() : 0;
        int remaining = Math.max(0, MAX_COMMENT_LENGTH - len);
        charCountText.setText(context.getString(R.string.ride_rating_char_count, remaining));
    }

    private void onSubmit() {
        if (submitting || submitButton == null) return;
        if (driverRating < 1 || vehicleRating < 1) {
            if (errorText != null) {
                errorText.setText(R.string.ride_rating_require_both);
                errorText.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (errorText != null) {
            errorText.setVisibility(View.GONE);
        }
        String comment = (commentInput != null && commentInput.getText() != null)
            ? commentInput.getText().toString().trim() : "";
        if (comment.isEmpty()) {
            comment = null;
        }

        submitting = true;
        submitButton.setEnabled(false);
        submitButton.setText(R.string.ride_rating_submitting);

        ReviewCreateRequest request = new ReviewCreateRequest(driverRating, vehicleRating, comment);
        rideApiService.createReview(rideId, request).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<ReviewResponse> call,
                @NonNull Response<ReviewResponse> response
            ) {
                if (dialog == null) return;
                submitting = false;
                if (submitButton != null) {
                    submitButton.setEnabled(true);
                    submitButton.setText(R.string.ride_rating_submit);
                }
                if (response.isSuccessful()) {
                    Toast.makeText(context, R.string.ride_rating_success, Toast.LENGTH_SHORT).show();
                    if (callbackHandler != null) {
                        callbackHandler.onReviewSubmitted();
                    }
                    dismiss();
                } else {
                    if (errorText != null) {
                        errorText.setText(R.string.ride_rating_failed);
                        errorText.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                if (dialog == null) return;
                submitting = false;
                if (submitButton != null) {
                    submitButton.setEnabled(true);
                    submitButton.setText(R.string.ride_rating_submit);
                }
                if (errorText != null) {
                    errorText.setText(R.string.ride_rating_failed);
                    errorText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
