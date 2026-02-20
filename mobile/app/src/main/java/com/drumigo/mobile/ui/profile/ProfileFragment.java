package com.drumigo.mobile.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.ProfileApiService;
import com.drumigo.mobile.data.model.profile.ActiveHoursResponse;
import com.drumigo.mobile.data.model.profile.PasswordUpdateRequest;
import com.drumigo.mobile.data.model.profile.ProfilePictureUploadResponse;
import com.drumigo.mobile.data.model.profile.ProfileResponse;
import com.drumigo.mobile.data.model.profile.VehicleInfoResponse;
import com.drumigo.mobile.databinding.FragmentProfileBinding;
import com.drumigo.mobile.session.SessionManager;
import com.drumigo.mobile.util.ProfilePictureUrlHelper;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Profile Fragment - Displays user profile from backend.
 * Role-based: Driver sees active hours, vehicle, pending banner; Passenger/Admin see same personal card only.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileApiService profileApiService;
    private SessionManager sessionManager;

    private ProfileResponse profile;
    private boolean hasPendingChangeRequest;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) uploadProfilePicture(uri);
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileApiService = ApiClient.getProfileApiService();
        sessionManager = SessionManager.getInstance(requireContext());

        setupRetryButton();
        setLoadingState(true);
        loadProfile();
        setupClickListeners();
    }

    private void setupRetryButton() {
        binding.profileRetryButton.setOnClickListener(v -> {
            setErrorState(false);
            setLoadingState(true);
            loadProfile();
        });
    }

    private void setLoadingState(boolean loading) {
        binding.profileLoadingContainer.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.profileErrorContainer.setVisibility(View.GONE);
        binding.profileContentContainer.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void setErrorState(boolean show) {
        binding.profileLoadingContainer.setVisibility(View.GONE);
        binding.profileErrorContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.profileContentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) {
            binding.profileErrorMessage.setText(R.string.profile_error_load);
        }
    }

    private void loadProfile() {
        if (!sessionManager.isAuthenticated()) {
            setErrorState(true);
            binding.profileErrorMessage.setText(R.string.profile_error_load);
            return;
        }
        profileApiService.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call,
                                  @NonNull Response<ProfileResponse> response) {
                if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                    profile = response.body();
                    setLoadingState(false);
                    applyProfileToUi();
                } else {
                    setErrorState(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    setErrorState(true);
                }
            }
        });
    }

    private void applyProfileToUi() {
        if (profile == null) return;

        String name = nullToEmpty(profile.name);
        String surname = nullToEmpty(profile.surname);
        String fullName = (name + " " + surname).trim();
        if (fullName.isEmpty()) fullName = getString(R.string.profile_title);

        binding.userName.setText(fullName);
        binding.firstName.setText(name);
        binding.lastName.setText(surname);
        binding.email.setText(nullToEmpty(profile.email));
        binding.phone.setText(nullToEmpty(profile.phone));
        binding.address.setText(nullToEmpty(profile.address));

        setRoleBadge(profile.role);
        loadAvatar(profile.profilePictureUrl, name, surname);

        if (isDriver()) {
            binding.activeHoursContainer.setVisibility(View.VISIBLE);
            applyActiveHours(profile.activeHoursLast24h);
            binding.pendingChangesCard.setVisibility(hasPendingChangeRequest ? View.VISIBLE : View.GONE);
            if (profile.vehicle != null) {
                binding.vehicleInfoCard.setVisibility(View.VISIBLE);
                applyVehicle(profile.vehicle);
            } else {
                binding.vehicleInfoCard.setVisibility(View.GONE);
            }
        } else {
            binding.activeHoursContainer.setVisibility(View.GONE);
            binding.vehicleInfoCard.setVisibility(View.GONE);
            binding.pendingChangesCard.setVisibility(View.GONE);
        }

        binding.btnEditVehicle.setOnClickListener(v -> showEditVehicleDialog());
    }

    private void setRoleBadge(String role) {
        if (role == null) role = "";
        switch (role.toUpperCase()) {
            case "DRIVER":
                binding.userRole.setText(R.string.profile_driver);
                break;
            case "ADMIN":
                binding.userRole.setText(R.string.profile_admin);
                break;
            default:
                binding.userRole.setText(R.string.profile_passenger);
                break;
        }
    }

    private void loadAvatar(String profilePictureUrl, String name, String surname) {
        String resolved = ProfilePictureUrlHelper.resolveProfilePictureUrl(
            profilePictureUrl, BuildConfig.API_BASE_URL);
        if (resolved != null && !resolved.isEmpty()) {
            binding.avatarInitials.setVisibility(View.GONE);
            binding.avatarImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(resolved)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .error(R.drawable.ic_default_avatar)
                .into(binding.avatarImage);
        } else {
            showDefaultAvatar();
        }
    }

    /** Universal default avatar when no profile picture (matches frontend default-avatar.svg). */
    private void showDefaultAvatar() {
        binding.avatarInitials.setVisibility(View.GONE);
        binding.avatarImage.setVisibility(View.VISIBLE);
        Glide.with(this).clear(binding.avatarImage);
        binding.avatarImage.setImageResource(R.drawable.ic_default_avatar);
        binding.avatarImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private static String initialsFrom(String name, String surname) {
        String n = name != null ? name.trim() : "";
        String s = surname != null ? surname.trim() : "";
        if (n.isEmpty() && s.isEmpty()) return "?";
        String a = n.isEmpty() ? "" : n.substring(0, 1).toUpperCase();
        String b = s.isEmpty() ? "" : s.substring(0, 1).toUpperCase();
        return (a + b).isEmpty() ? "?" : (a + b);
    }

    private void applyActiveHours(@Nullable ActiveHoursResponse activeHours) {
        double hours = activeHours != null ? activeHours.hoursWorked : 0;
        int maxHours = activeHours != null ? activeHours.maxHours : 8;
        int percent = maxHours > 0 ? (int) Math.round((hours / maxHours) * 100) : 0;
        percent = Math.min(100, percent);

        int h = (int) hours;
        int m = (int) ((hours - h) * 60);
        String timeText = h + "h " + m + "m";
        binding.activeHoursValue.setText(timeText);
        binding.activeHoursProgress.setProgress(percent);
        binding.activeHoursProgressText.setText(
            getString(R.string.active_hours_progress, percent));
    }

    private void applyVehicle(VehicleInfoResponse v) {
        binding.vehicleName.setText(v.model != null ? v.model : "");
        binding.vehicleType.setText(vehicleTypeDisplay(v.vehicleTypeName));
        binding.licensePlate.setText(v.licensePlate != null ? v.licensePlate : "");
        int seats = v.numSeats != null ? v.numSeats : 0;
        binding.seatsCount.setText(getString(R.string.seats_count, seats));
        binding.featureBabySeats.setVisibility(Boolean.TRUE.equals(v.babyFriendly) ? View.VISIBLE : View.GONE);
        binding.featurePetFriendly.setVisibility(Boolean.TRUE.equals(v.petFriendly) ? View.VISIBLE : View.GONE);
    }

    private static String vehicleTypeDisplay(String vehicleTypeName) {
        if (vehicleTypeName == null) return "Standard";
        switch (vehicleTypeName.toUpperCase()) {
            case "LUXURY": return "Luxury";
            case "VAN": return "Van";
            default: return "Standard";
        }
    }

    private boolean isDriver() {
        return profile != null && "DRIVER".equalsIgnoreCase(profile.role);
    }

    private void setupClickListeners() {
        binding.avatarEditBadge.setOnClickListener(v -> showPhotoOptions());
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void showPhotoOptions() {
        if (pickImageLauncher != null) pickImageLauncher.launch("image/*");
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (profileApiService == null || !isAdded()) return;
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
            if (is == null) {
                Snackbar.make(binding.getRoot(), R.string.error_update_profile_picture, Snackbar.LENGTH_SHORT).show();
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int read;
            while ((read = is.read(b)) != -1) buffer.write(b, 0, read);
            is.close();
            byte[] bytes = buffer.toByteArray();
            if (bytes.length == 0) {
                Snackbar.make(binding.getRoot(), R.string.error_update_profile_picture, Snackbar.LENGTH_SHORT).show();
                return;
            }
            final int maxBytes = 5 * 1024 * 1024; // 5MB
            if (bytes.length > maxBytes) {
                Snackbar.make(binding.getRoot(), R.string.error_profile_picture_too_large, Snackbar.LENGTH_LONG).show();
                return;
            }
            // Use image/jpeg and .jpg so backend always accepts (avoids content-type issues with picker/ngrok)
            String mimeType = "image/jpeg";
            String filename = "photo.jpg";
            RequestBody body = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, body);
            profileApiService.uploadProfilePicture(part).enqueue(new Callback<ProfilePictureUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ProfilePictureUploadResponse> call,
                                      @NonNull Response<ProfilePictureUploadResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().url != null && !response.body().url.isEmpty()) {
                        String url = response.body().url.trim();
                        com.drumigo.mobile.data.model.profile.ProfileUpdateRequest req =
                            new com.drumigo.mobile.data.model.profile.ProfileUpdateRequest(
                                profile != null ? profile.name : null,
                                profile != null ? profile.surname : null,
                                profile != null ? profile.email : null,
                                profile != null ? profile.address : null,
                                profile != null ? profile.phone : null,
                                url);
                        profileApiService.updateProfile(req).enqueue(new Callback<ProfileResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<ProfileResponse> c, @NonNull Response<ProfileResponse> r) {
                                if (!isAdded()) return;
                                if (r.isSuccessful() && r.body() != null) {
                                    profile = r.body();
                                    applyProfileToUi();
                                    Snackbar.make(binding.getRoot(), R.string.photo_updated, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    showProfilePictureError(r.errorBody(), r.code());
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<ProfileResponse> c, @NonNull Throwable t) {
                                if (isAdded()) showProfilePictureError(null, -1, t);
                            }
                        });
                    } else if (response.isSuccessful() && (response.body() == null || response.body().url == null || response.body().url.isEmpty())) {
                        showProfilePictureError(null, 200, null);
                    } else {
                        showProfilePictureError(response.errorBody(), response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ProfilePictureUploadResponse> call, @NonNull Throwable t) {
                    if (isAdded()) showProfilePictureError(null, -1, t);
                }
            });
        } catch (Exception e) {
            String detail = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().isEmpty()) detail += ": " + e.getMessage();
            Snackbar.make(binding.getRoot(), getString(R.string.error_update_profile_picture) + " (" + detail + ")", Snackbar.LENGTH_LONG).show();
        }
    }

    /** Extract string value of a key from JSON, e.g. "error" or "message". */
    private static String parseJsonMessage(String json, String key) {
        if (json == null || key == null) return null;
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        int valueStart = json.indexOf(":", start);
        if (valueStart == -1) return null;
        int quoteStart = json.indexOf("\"", valueStart);
        if (quoteStart == -1) return null;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private void showProfilePictureError(okhttp3.ResponseBody errorBody, int responseCode) {
        showProfilePictureError(errorBody, responseCode, null);
    }

    private void showProfilePictureError(okhttp3.ResponseBody errorBody, int responseCode, Throwable networkError) {
        String message = getString(R.string.error_update_profile_picture);
        String rawBody = null;
        if (networkError != null) {
            String errMsg = networkError.getMessage();
            message = networkError.getClass().getSimpleName() + (errMsg != null && !errMsg.isEmpty() ? ": " + errMsg : "");
        } else if (errorBody != null) {
            try {
                String json = errorBody.string();
                if (json != null && !json.isEmpty()) {
                    String parsed = parseJsonMessage(json, "error");
                    if (parsed == null) parsed = parseJsonMessage(json, "message");
                    if (parsed != null) {
                        message = parsed;
                    } else {
                        rawBody = json.length() > 120 ? json.substring(0, 120) + "..." : json;
                    }
                }
            } catch (Exception ignored) { }
        }
        if (message.equals(getString(R.string.error_update_profile_picture))) {
            if (responseCode == 200) message = getString(R.string.error_update_profile_picture) + " (HTTP 200, invalid response - check API URL or ngrok)";
            else if (responseCode == 401) message = getString(R.string.error_update_profile_picture) + " (HTTP 401 Unauthorized)";
            else if (responseCode == 403) message = getString(R.string.error_update_profile_picture) + " (HTTP 403 Forbidden)";
            else if (responseCode == 404) message = getString(R.string.error_update_profile_picture) + " (HTTP 404)";
            else if (responseCode >= 500) message = getString(R.string.error_update_profile_picture) + " (HTTP " + responseCode + " Server error)";
            else if (responseCode > 0) message = getString(R.string.error_update_profile_picture) + " (HTTP " + responseCode + ")";
            else message = getString(R.string.error_update_profile_picture) + " (no response code)";
        }
        if (rawBody != null) message = message + " [" + rawBody.replace("\n", " ") + "]";
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void showChangePasswordDialog() {
        if (getContext() == null || profileApiService == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        TextInputEditText editCurrentPassword = dialogView.findViewById(R.id.editCurrentPassword);
        TextInputEditText editNewPassword = dialogView.findViewById(R.id.editNewPassword);
        TextInputEditText editConfirmPassword = dialogView.findViewById(R.id.editConfirmPassword);

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelPassword);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        MaterialButton btnChangePassword = dialogView.findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                String current = editCurrentPassword != null && editCurrentPassword.getText() != null
                    ? editCurrentPassword.getText().toString() : "";
                String newPw = editNewPassword != null && editNewPassword.getText() != null
                    ? editNewPassword.getText().toString() : "";
                String confirm = editConfirmPassword != null && editConfirmPassword.getText() != null
                    ? editConfirmPassword.getText().toString() : "";
                String err = validateChangePassword(current, newPw, confirm);
                if (err != null) {
                    Snackbar.make(binding.getRoot(), err, Snackbar.LENGTH_LONG).show();
                    return;
                }
                profileApiService.updatePassword(new PasswordUpdateRequest(current, newPw))
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (!isAdded()) return;
                            dialog.dismiss();
                            if (response.isSuccessful()) {
                                Snackbar.make(binding.getRoot(), R.string.password_changed, Snackbar.LENGTH_SHORT).show();
                            } else {
                                showPasswordError(response.errorBody(), response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            if (isAdded()) {
                                dialog.dismiss();
                                String msg = t.getMessage() != null && !t.getMessage().isEmpty()
                                    ? t.getMessage() : getString(R.string.error_change_password);
                                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
            });
        }
        dialog.show();
    }

    /** Same rules as backend: 6–64 chars, at least one lowercase, one uppercase, one digit or special. */
    private static boolean isNewPasswordValid(String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) return false;
        return newPassword.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9\\W]).{6,64}");
    }

    private String validateChangePassword(String current, String newPw, String confirm) {
        if (current == null || current.trim().isEmpty())
            return getString(R.string.validation_current_password_required);
        if (newPw == null || newPw.isEmpty())
            return getString(R.string.validation_new_password_required);
        if (!isNewPasswordValid(newPw))
            return getString(R.string.validation_password_requirements);
        if (!newPw.equals(confirm))
            return getString(R.string.validation_passwords_must_match);
        return null;
    }

    private void showPasswordError(okhttp3.ResponseBody errorBody, int responseCode) {
        String message = getString(R.string.error_change_password);
        if (errorBody != null) {
            try {
                String json = errorBody.string();
                if (json != null && json.contains("\"message\"")) {
                    int start = json.indexOf("\"message\"");
                    int valueStart = json.indexOf(":", start);
                    if (valueStart != -1) {
                        int quoteStart = json.indexOf("\"", valueStart);
                        if (quoteStart != -1) {
                            int quoteEnd = json.indexOf("\"", quoteStart + 1);
                            if (quoteEnd != -1) message = json.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                } else if (json != null && json.contains("\"error\"")) {
                    int start = json.indexOf("\"error\"");
                    int valueStart = json.indexOf(":", start);
                    if (valueStart != -1) {
                        int quoteStart = json.indexOf("\"", valueStart);
                        if (quoteStart != -1) {
                            int quoteEnd = json.indexOf("\"", quoteStart + 1);
                            if (quoteEnd != -1) message = json.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        if (message.equals(getString(R.string.error_change_password)) && responseCode == 400)
            message = getString(R.string.error_change_password) + " (Bad request)";
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void showEditProfileDialog() {
        if (getContext() == null || profile == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(dialogView);

        TextInputEditText editFirstName = dialogView.findViewById(R.id.editFirstName);
        TextInputEditText editLastName = dialogView.findViewById(R.id.editLastName);
        TextInputEditText editEmail = dialogView.findViewById(R.id.editEmail);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editAddress = dialogView.findViewById(R.id.editAddress);
        if (editFirstName != null) editFirstName.setText(nullToEmpty(profile.name));
        if (editLastName != null) editLastName.setText(nullToEmpty(profile.surname));
        if (editEmail != null) editEmail.setText(nullToEmpty(profile.email));
        if (editPhone != null) editPhone.setText(nullToEmpty(profile.phone));
        if (editAddress != null) editAddress.setText(nullToEmpty(profile.address));

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelEdit);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveChanges);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                savePersonalInfoFromDialog(editFirstName, editLastName, editEmail, editPhone, editAddress, dialog);
            });
        }
        dialog.show();
    }

    private void savePersonalInfoFromDialog(TextInputEditText editFirstName,
                                            TextInputEditText editLastName,
                                            TextInputEditText editEmail,
                                            TextInputEditText editPhone,
                                            TextInputEditText editAddress,
                                            BottomSheetDialog dialog) {
        String name = editFirstName != null && editFirstName.getText() != null ? editFirstName.getText().toString().trim() : "";
        String surname = editLastName != null && editLastName.getText() != null ? editLastName.getText().toString().trim() : "";
        String email = editEmail != null && editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String phone = editPhone != null && editPhone.getText() != null ? editPhone.getText().toString().trim() : "";
        String address = editAddress != null && editAddress.getText() != null ? editAddress.getText().toString().trim() : "";
        if (isDriver()) {
            submitDriverProfileChangeRequest(name, surname, email, phone, address, dialog);
        } else {
            updateProfilePut(name, surname, email, phone, address, dialog);
        }
    }

    private void updateProfilePut(String name, String surname, String email, String phone,
                                  String address, BottomSheetDialog dialog) {
        com.drumigo.mobile.data.model.profile.ProfileUpdateRequest req =
            new com.drumigo.mobile.data.model.profile.ProfileUpdateRequest(
                name, surname, email, address, phone, profile != null ? profile.profilePictureUrl : null);
        profileApiService.updateProfile(req).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call,
                                  @NonNull Response<ProfileResponse> response) {
                if (!isAdded()) return;
                dialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    profile = response.body();
                    applyProfileToUi();
                    Snackbar.make(binding.getRoot(), R.string.profile_updated, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void submitDriverProfileChangeRequest(String name, String surname, String email,
                                                  String phone, String address, BottomSheetDialog dialog) {
        long driverId = sessionManager.getUserId();
        if (driverId <= 0) {
            dialog.dismiss();
            return;
        }
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("name", name);
            json.put("surname", surname);
            json.put("email", email);
            json.put("phone", phone);
            json.put("address", address);
            com.drumigo.mobile.data.model.DriverProfileChangeRequestCreateRequest req =
                new com.drumigo.mobile.data.model.DriverProfileChangeRequestCreateRequest(json.toString());
            ApiClient.getDriverApiService().createProfileChangeRequest(driverId, req)
                .enqueue(new Callback<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse> call,
                                          @NonNull Response<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse> response) {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        if (response.isSuccessful()) {
                            hasPendingChangeRequest = true;
                            binding.pendingChangesCard.setVisibility(View.VISIBLE);
                            Snackbar.make(binding.getRoot(), R.string.profile_update_pending, Snackbar.LENGTH_LONG).show();
                        } else {
                            Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse> call,
                                         @NonNull Throwable t) {
                        if (isAdded()) {
                            dialog.dismiss();
                            Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
        } catch (Exception e) {
            dialog.dismiss();
            Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showEditVehicleDialog() {
        if (getContext() == null || profile == null || profile.vehicle == null) return;
        VehicleInfoResponse v = profile.vehicle;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_vehicle, null);
        dialog.setContentView(dialogView);

        TextInputEditText editVehicleName = dialogView.findViewById(R.id.editVehicleName);
        android.widget.AutoCompleteTextView editVehicleType = dialogView.findViewById(R.id.editVehicleType);
        TextInputEditText editLicensePlate = dialogView.findViewById(R.id.editLicensePlate);
        android.widget.AutoCompleteTextView editSeats = dialogView.findViewById(R.id.editSeats);
        com.google.android.material.checkbox.MaterialCheckBox checkBabySeats = dialogView.findViewById(R.id.checkBabySeats);
        com.google.android.material.checkbox.MaterialCheckBox checkPetFriendly = dialogView.findViewById(R.id.checkPetFriendly);

        if (editVehicleName != null) editVehicleName.setText(v.model != null ? v.model : "");
        if (editVehicleType != null) editVehicleType.setText(vehicleTypeDisplay(v.vehicleTypeName));
        if (editLicensePlate != null) editLicensePlate.setText(v.licensePlate != null ? v.licensePlate : "");
        if (editSeats != null) editSeats.setText(String.valueOf(v.numSeats != null ? v.numSeats : 4));
        if (checkBabySeats != null) checkBabySeats.setChecked(Boolean.TRUE.equals(v.babyFriendly));
        if (checkPetFriendly != null) checkPetFriendly.setChecked(Boolean.TRUE.equals(v.petFriendly));

        dialogView.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelVehicle);
        if (btnCancel != null) btnCancel.setOnClickListener(view -> dialog.dismiss());
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveVehicle);
        if (btnSave != null) {
            btnSave.setOnClickListener(view -> {
                String model = editVehicleName != null && editVehicleName.getText() != null ? editVehicleName.getText().toString().trim() : "";
                String typeDisplay = editVehicleType != null && editVehicleType.getText() != null ? editVehicleType.getText().toString().trim() : "Standard";
                String licensePlate = editLicensePlate != null && editLicensePlate.getText() != null ? editLicensePlate.getText().toString().trim() : "";
                int seats = 4;
                if (editSeats != null && editSeats.getText() != null) {
                    try {
                        seats = Integer.parseInt(editSeats.getText().toString().trim());
                    } catch (NumberFormatException ignored) { }
                }
                boolean babySeats = checkBabySeats != null && checkBabySeats.isChecked();
                boolean petFriendly = checkPetFriendly != null && checkPetFriendly.isChecked();
                String vehicleTypeName = vehicleTypeToBackend(typeDisplay);
                submitVehicleChangeRequest(model, vehicleTypeName, licensePlate, seats, babySeats, petFriendly, dialog);
            });
        }
        dialog.show();
    }

    private static String vehicleTypeToBackend(String display) {
        if (display == null) return "STANDARD";
        switch (display.trim().toLowerCase()) {
            case "luxury": return "LUXURY";
            case "van": return "VAN";
            default: return "STANDARD";
        }
    }

    private void submitVehicleChangeRequest(String model, String vehicleTypeName, String licensePlate,
                                            int seats, boolean babySeats, boolean petFriendly,
                                            BottomSheetDialog dialog) {
        long driverId = sessionManager.getUserId();
        if (driverId <= 0) {
            dialog.dismiss();
            return;
        }
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("model", model);
            json.put("vehicleTypeName", vehicleTypeName);
            json.put("licensePlate", licensePlate);
            json.put("numSeats", seats);
            json.put("babyFriendly", babySeats);
            json.put("petFriendly", petFriendly);
            com.drumigo.mobile.data.model.DriverProfileChangeRequestCreateRequest req =
                new com.drumigo.mobile.data.model.DriverProfileChangeRequestCreateRequest(json.toString());
            ApiClient.getDriverApiService().createProfileChangeRequest(driverId, req)
                .enqueue(new Callback<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse> call,
                                          @NonNull Response<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse> response) {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        if (response.isSuccessful()) {
                            hasPendingChangeRequest = true;
                            binding.pendingChangesCard.setVisibility(View.VISIBLE);
                            Snackbar.make(binding.getRoot(), R.string.profile_update_pending, Snackbar.LENGTH_LONG).show();
                        } else {
                            Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.drumigo.mobile.data.model.DriverProfileChangeRequestResponse> call,
                                         @NonNull Throwable t) {
                        if (isAdded()) {
                            dialog.dismiss();
                            Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
        } catch (Exception e) {
            dialog.dismiss();
            Snackbar.make(binding.getRoot(), R.string.error_update_profile, Snackbar.LENGTH_SHORT).show();
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
