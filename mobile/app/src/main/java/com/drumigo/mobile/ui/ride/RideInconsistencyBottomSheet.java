package com.drumigo.mobile.ui.ride;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.RideApiService;
import com.drumigo.mobile.data.model.ride.RideInconsistencyCreateRequest;
import com.drumigo.mobile.data.model.ride.RideInconsistencyResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RideInconsistencyBottomSheet {

    private static final int MIN_NOTE_LENGTH = 10;

    interface CallbackHandler {
        void onReportSubmitted();
    }

    private final Context context;
    private final RideApiService rideApiService;
    private final long rideId;
    private final CallbackHandler callbackHandler;

    private BottomSheetDialog dialog;
    private TextInputEditText noteInput;
    private TextView errorText;
    private MaterialButton submitButton;
    private boolean submitting;

    private RideInconsistencyBottomSheet(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @NonNull CallbackHandler callbackHandler
    ) {
        this.context = context;
        this.rideApiService = rideApiService;
        this.rideId = rideId;
        this.callbackHandler = callbackHandler;
    }

    static void show(
        @NonNull Context context,
        @NonNull RideApiService rideApiService,
        long rideId,
        @NonNull CallbackHandler callbackHandler
    ) {
        new RideInconsistencyBottomSheet(context, rideApiService, rideId, callbackHandler).showInternal();
    }

    private void showInternal() {
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_ride_inconsistency);
        dialog.setDismissWithAnimation(true);

        noteInput = dialog.findViewById(R.id.inconsistencyNoteInput);
        errorText = dialog.findViewById(R.id.inconsistencyErrorText);
        submitButton = dialog.findViewById(R.id.inconsistencySubmitButton);
        View closeButton = dialog.findViewById(R.id.inconsistencyCloseButton);

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
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

    private void onSubmit() {
        if (submitting || noteInput == null || submitButton == null || errorText == null) {
            return;
        }
        String note = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
        if (note.length() < MIN_NOTE_LENGTH) {
            errorText.setText(context.getString(R.string.ride_tracking_inconsistency_note_too_short, MIN_NOTE_LENGTH));
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        errorText.setVisibility(View.GONE);
        submitting = true;
        submitButton.setEnabled(false);
        submitButton.setText(R.string.ride_tracking_inconsistency_submitting);

        rideApiService.reportInconsistency(rideId, new RideInconsistencyCreateRequest(note))
            .enqueue(new Callback<RideInconsistencyResponse>() {
                @Override
                public void onResponse(
                    @NonNull Call<RideInconsistencyResponse> call,
                    @NonNull Response<RideInconsistencyResponse> response
                ) {
                    if (dialog == null) {
                        return;
                    }
                    submitting = false;
                    if (submitButton != null) {
                        submitButton.setEnabled(true);
                        submitButton.setText(R.string.ride_tracking_inconsistency_submit);
                    }
                    if (response.isSuccessful()) {
                        if (callbackHandler != null) {
                            callbackHandler.onReportSubmitted();
                        }
                        dismiss();
                    } else {
                        if (errorText != null) {
                            errorText.setText(R.string.ride_tracking_inconsistency_failed);
                            errorText.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<RideInconsistencyResponse> call, @NonNull Throwable t) {
                    if (dialog == null) {
                        return;
                    }
                    submitting = false;
                    if (submitButton != null) {
                        submitButton.setEnabled(true);
                        submitButton.setText(R.string.ride_tracking_inconsistency_submit);
                    }
                    if (errorText != null) {
                        errorText.setText(R.string.ride_tracking_inconsistency_failed);
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
