package com.drumigo.mobile.ui.admin;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.DriverApiService;
import com.drumigo.mobile.data.model.DriverCreateRequest;
import com.drumigo.mobile.data.model.profile.ProfilePictureUploadResponse;
import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.data.remote.AuthApi;
import com.drumigo.mobile.databinding.FragmentRegisterDriverBinding;
import com.drumigo.mobile.util.ProfilePictureUrlHelper;

import java.util.regex.Pattern;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Admin-only screen to register a new driver (personal info + vehicle).
 * Uses POST /api/auth/profile-picture (no auth) for photo, then POST /api/drivers (JWT) to create driver.
 */
public class RegisterDriverFragment extends Fragment {

    private static final String[] VEHICLE_CATEGORIES = new String[] { "Standard", "Luxury", "Van" };
    /** Same as frontend: letters, digits, hyphen, space; 4-12 chars. */
    private static final Pattern LICENSE_PLATE_PATTERN = Pattern.compile("^[A-Z0-9\\-\\s]{4,12}$");
    private static final int SEATS_MIN = 1;
    private static final int SEATS_MAX = 8;
    private static final int SEATS_DEFAULT = 4;
    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024; // 5MB

    private FragmentRegisterDriverBinding binding;
    private DriverApiService driverApiService;
    private AuthApi authApi;

    private int currentStep = 1;
    private int seats = SEATS_DEFAULT;
    private String profilePictureUrl = null;
    private Uri selectedPhotoUri = null;
    private boolean isSubmitting = false;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    showDriverPhoto(uri);
                    uploadProfilePicture(uri);
                }
            }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterDriverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        driverApiService = ApiClient.getDriverApiService();
        authApi = com.drumigo.mobile.data.remote.ApiClient.getAuthApi();

        setupStepIndicator();
        setupVehicleCategoryDropdown();
        setupLicensePlateInput();
        setupSeatsStepper();
        setupClickListeners();
        showDefaultDriverPhoto();
    }

    /** Show selected photo (Uri) or uploaded URL in avatar, same style as profile. */
    private void showDriverPhoto(Uri uri) {
        if (binding == null || !isAdded()) return;
        Glide.with(this)
            .load(uri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
            .error(R.drawable.ic_default_avatar)
            .into(binding.driverPhotoImage);
    }

    /** Show photo from URL after upload (e.g. from backend). */
    private void showDriverPhotoFromUrl(String url) {
        if (binding == null || !isAdded() || url == null || url.trim().isEmpty()) return;
        String resolved = ProfilePictureUrlHelper.resolveProfilePictureUrl(url, BuildConfig.API_BASE_URL);
        if (resolved == null || resolved.isEmpty()) return;
        Glide.with(this)
            .load(resolved)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
            .error(R.drawable.ic_default_avatar)
            .into(binding.driverPhotoImage);
    }

    /** Reset avatar to default placeholder (no photo). */
    private void showDefaultDriverPhoto() {
        if (binding == null) return;
        Glide.with(this).clear(binding.driverPhotoImage);
        binding.driverPhotoImage.setImageResource(R.drawable.ic_default_avatar);
        binding.driverPhotoImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void setupStepIndicator() {
        updateStepIndicator();
    }

    private void updateStepIndicator() {
        if (binding.stepIndicator != null) {
            binding.stepIndicator.setText(getString(R.string.register_driver_step1_title) + " — " + getString(R.string.register_driver_step2_title));
        }
    }

    private void setupVehicleCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, VEHICLE_CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.vehicleCategorySpinner.setAdapter(adapter);
        binding.vehicleCategorySpinner.setSelection(0);
    }

    /**
     * Force license plate format to match frontend: only A–Z, 0–9, hyphen, space; max 12 chars; uppercase.
     */
    private void setupLicensePlateInput() {
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (Character.isLetterOrDigit(c) || c == '-' || c == ' ') {
                    sb.append(Character.toUpperCase(c));
                }
            }
            if (sb.length() == 0) return "";
            if (sb.length() == end - start) {
                String orig = source.subSequence(start, end).toString();
                if (orig.equals(sb.toString())) return null;
            }
            return sb.toString();
        };
        binding.licensePlateInput.setFilters(new InputFilter[] {
            new InputFilter.LengthFilter(12),
            filter
        });
    }

    private void setupSeatsStepper() {
        seats = SEATS_DEFAULT;
        binding.seatsValue.setText(String.valueOf(seats));
        binding.seatsMinusButton.setOnClickListener(v -> {
            if (seats > SEATS_MIN) {
                seats--;
                binding.seatsValue.setText(String.valueOf(seats));
            }
        });
        binding.seatsPlusButton.setOnClickListener(v -> {
            if (seats < SEATS_MAX) {
                seats++;
                binding.seatsValue.setText(String.valueOf(seats));
            }
        });
    }

    private void setupClickListeners() {
        binding.uploadPhotoButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.continueButton.setOnClickListener(v -> {
            if (validateStep1()) {
                currentStep = 2;
                binding.step1Container.setVisibility(View.GONE);
                binding.step2Container.setVisibility(View.VISIBLE);
            }
        });

        binding.backButton.setOnClickListener(v -> {
            currentStep = 1;
            binding.step2Container.setVisibility(View.GONE);
            binding.step1Container.setVisibility(View.VISIBLE);
        });

        binding.registerButton.setOnClickListener(v -> submitRegistration());

        binding.registerAnotherButton.setOnClickListener(v -> resetForm());
    }

    private boolean validateStep1() {
        String firstName = getText(binding.firstNameInput);
        String lastName = getText(binding.lastNameInput);
        String email = getText(binding.emailInput);
        String phone = getText(binding.phoneInput);
        String address = getText(binding.addressInput);

        clearError(binding.firstNameInputLayout);
        clearError(binding.lastNameInputLayout);
        clearError(binding.emailInputLayout);
        clearError(binding.phoneInputLayout);
        clearError(binding.addressInputLayout);

        boolean valid = true;
        if (firstName.isEmpty()) {
            binding.firstNameInputLayout.setError(getString(R.string.error_first_name_required));
            binding.firstNameInputLayout.setErrorEnabled(true);
            valid = false;
        }
        if (lastName.isEmpty()) {
            binding.lastNameInputLayout.setError(getString(R.string.error_last_name_required));
            binding.lastNameInputLayout.setErrorEnabled(true);
            valid = false;
        }
        if (email.isEmpty()) {
            binding.emailInputLayout.setError(getString(R.string.error_email_required));
            binding.emailInputLayout.setErrorEnabled(true);
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.register_driver_validation_email));
            binding.emailInputLayout.setErrorEnabled(true);
            valid = false;
        }
        if (phone.isEmpty()) {
            binding.phoneInputLayout.setError(getString(R.string.error_phone_required));
            binding.phoneInputLayout.setErrorEnabled(true);
            valid = false;
        }
        if (address.isEmpty()) {
            binding.addressInputLayout.setError(getString(R.string.error_address_required));
            binding.addressInputLayout.setErrorEnabled(true);
            valid = false;
        }
        return valid;
    }

    private boolean validateStep2() {
        String model = getText(binding.vehicleModelInput);
        String licensePlate = getText(binding.licensePlateInput).trim();

        clearError(binding.vehicleModelInputLayout);
        clearError(binding.licensePlateInputLayout);

        boolean valid = true;
        if (model.isEmpty()) {
            binding.vehicleModelInputLayout.setError(getString(R.string.register_driver_vehicle_model_required));
            binding.vehicleModelInputLayout.setErrorEnabled(true);
            valid = false;
        }
        if (licensePlate == null || licensePlate.length() < 4 || licensePlate.length() > 12
            || !LICENSE_PLATE_PATTERN.matcher(licensePlate.trim()).matches()) {
            binding.licensePlateInputLayout.setError(getString(R.string.register_driver_validation_license));
            binding.licensePlateInputLayout.setErrorEnabled(true);
            valid = false;
        }
        if (seats < SEATS_MIN || seats > SEATS_MAX) {
            valid = false;
        }
        return valid;
    }

    private void submitRegistration() {
        if (isSubmitting) return;
        if (!validateStep1() || !validateStep2()) {
            if (currentStep != 2) {
                currentStep = 1;
                binding.step2Container.setVisibility(View.GONE);
                binding.step1Container.setVisibility(View.VISIBLE);
            }
            return;
        }

        isSubmitting = true;
        binding.registerButton.setEnabled(false);
        binding.registerButton.setText(R.string.register_driver_submitting);

        String name = getText(binding.firstNameInput).trim();
        String surname = getText(binding.lastNameInput).trim();
        String email = getText(binding.emailInput).trim();
        String address = getText(binding.addressInput).trim();
        String countryCode = getText(binding.countryCodeInput).trim();
        String phoneLocal = getText(binding.phoneInput).trim();
        String phone = (countryCode.isEmpty() ? "" : countryCode) + phoneLocal;

        String category = (String) binding.vehicleCategorySpinner.getSelectedItem();
        long vehicleTypeId = categoryToVehicleTypeId(category);
        String vehicleModel = getText(binding.vehicleModelInput).trim();
        String licensePlate = getText(binding.licensePlateInput).trim().toUpperCase();

        if (selectedPhotoUri != null && profilePictureUrl == null) {
            uploadThenRegister(name, surname, email, address, phone, vehicleTypeId, vehicleModel, licensePlate);
            return;
        }

        doRegister(name, surname, email, address, phone, profilePictureUrl, vehicleTypeId, vehicleModel, licensePlate);
    }

    private void uploadThenRegister(String name, String surname, String email, String address, String phone,
                                    long vehicleTypeId, String vehicleModel, String licensePlate) {
        if (selectedPhotoUri == null) {
            doRegister(name, surname, email, address, phone, null, vehicleTypeId, vehicleModel, licensePlate);
            return;
        }
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(selectedPhotoUri);
            if (is == null) {
                doRegister(name, surname, email, address, phone, null, vehicleTypeId, vehicleModel, licensePlate);
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int read;
            while ((read = is.read(b)) != -1) buffer.write(b, 0, read);
            is.close();
            byte[] bytes = buffer.toByteArray();
            if (bytes.length == 0 || bytes.length > MAX_PHOTO_BYTES) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.register_driver_upload_photo_failed, Toast.LENGTH_SHORT).show();
                }
                doRegister(name, surname, email, address, phone, null, vehicleTypeId, vehicleModel, licensePlate);
                return;
            }
            RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", "photo.jpg", body);
            authApi.uploadProfilePictureForRegistration(part).enqueue(new Callback<ProfilePictureUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ProfilePictureUploadResponse> call,
                                      @NonNull Response<ProfilePictureUploadResponse> response) {
                    if (!isAdded()) return;
                    String url = null;
                    if (response.isSuccessful() && response.body() != null && response.body().url != null && !response.body().url.isEmpty()) {
                        url = response.body().url.trim();
                    } else {
                        Toast.makeText(requireContext(), R.string.register_driver_upload_photo_failed, Toast.LENGTH_SHORT).show();
                    }
                    doRegister(name, surname, email, address, phone, url, vehicleTypeId, vehicleModel, licensePlate);
                }

                @Override
                public void onFailure(@NonNull Call<ProfilePictureUploadResponse> call, @NonNull Throwable t) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.register_driver_upload_photo_failed, Toast.LENGTH_SHORT).show();
                        doRegister(name, surname, email, address, phone, null, vehicleTypeId, vehicleModel, licensePlate);
                    }
                }
            });
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.register_driver_upload_photo_failed, Toast.LENGTH_SHORT).show();
                doRegister(name, surname, email, address, phone, null, vehicleTypeId, vehicleModel, licensePlate);
            }
        }
    }

    private void doRegister(String name, String surname, String email, String address, String phone,
                            String profilePictureUrl, long vehicleTypeId, String vehicleModel, String licensePlate) {
        DriverCreateRequest request = new DriverCreateRequest(
            name,
            surname,
            email,
            address,
            phone,
            profilePictureUrl,
            vehicleTypeId,
            vehicleModel,
            licensePlate,
            seats,
            binding.babySeatsCheckbox.isChecked(),
            binding.petFriendlyCheckbox.isChecked()
        );

        driverApiService.createDriver(request).enqueue(new Callback<com.drumigo.mobile.data.model.DriverResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.drumigo.mobile.data.model.DriverResponse> call,
                                  @NonNull Response<com.drumigo.mobile.data.model.DriverResponse> response) {
                if (!isAdded()) return;
                isSubmitting = false;
                binding.registerButton.setEnabled(true);
                binding.registerButton.setText(R.string.register_driver_submit);

                if (response.isSuccessful()) {
                    binding.step1Container.setVisibility(View.GONE);
                    binding.step2Container.setVisibility(View.GONE);
                    binding.successContainer.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), R.string.register_driver_success, Toast.LENGTH_SHORT).show();
                } else {
                    String message = getString(R.string.register_driver_failed);
                    if (response.errorBody() != null) {
                        try {
                            String body = response.errorBody().string();
                            if (body.contains("\"message\"")) {
                                int start = body.indexOf("\"message\"");
                                int valueStart = body.indexOf(":", start);
                                int q = body.indexOf("\"", valueStart + 1);
                                int qEnd = body.indexOf("\"", q + 1);
                                if (q > 0 && qEnd > q) {
                                    message = body.substring(q + 1, qEnd);
                                }
                            }
                        } catch (Exception ignored) { }
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.drumigo.mobile.data.model.DriverResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                isSubmitting = false;
                binding.registerButton.setEnabled(true);
                binding.registerButton.setText(R.string.register_driver_submit);
                Toast.makeText(requireContext(), getString(R.string.register_driver_failed) + " " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (authApi == null || !isAdded()) return;
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
            if (is == null) return;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int read;
            while ((read = is.read(b)) != -1) buffer.write(b, 0, read);
            is.close();
            byte[] bytes = buffer.toByteArray();
            if (bytes.length == 0 || bytes.length > MAX_PHOTO_BYTES) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_profile_picture_too_large, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", "photo.jpg", body);
            authApi.uploadProfilePictureForRegistration(part).enqueue(new Callback<ProfilePictureUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ProfilePictureUploadResponse> call,
                                      @NonNull Response<ProfilePictureUploadResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().url != null && !response.body().url.isEmpty()) {
                        profilePictureUrl = response.body().url.trim();
                        showDriverPhotoFromUrl(profilePictureUrl);
                        Toast.makeText(requireContext(), R.string.upload_photo, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ProfilePictureUploadResponse> call, @NonNull Throwable t) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.register_driver_upload_photo_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.register_driver_upload_photo_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetForm() {
        binding.successContainer.setVisibility(View.GONE);
        binding.step1Container.setVisibility(View.VISIBLE);
        binding.step2Container.setVisibility(View.GONE);
        currentStep = 1;
        seats = SEATS_DEFAULT;
        binding.seatsValue.setText(String.valueOf(seats));
        profilePictureUrl = null;
        selectedPhotoUri = null;
        showDefaultDriverPhoto();

        binding.firstNameInput.setText("");
        binding.lastNameInput.setText("");
        binding.emailInput.setText("");
        binding.countryCodeInput.setText(getString(R.string.country_code_default));
        binding.phoneInput.setText("");
        binding.addressInput.setText("");
        binding.vehicleModelInput.setText("");
        binding.vehicleCategorySpinner.setSelection(0);
        binding.licensePlateInput.setText("");
        binding.babySeatsCheckbox.setChecked(false);
        binding.petFriendlyCheckbox.setChecked(false);

        clearError(binding.firstNameInputLayout);
        clearError(binding.lastNameInputLayout);
        clearError(binding.emailInputLayout);
        clearError(binding.phoneInputLayout);
        clearError(binding.addressInputLayout);
        clearError(binding.vehicleModelInputLayout);
        clearError(binding.licensePlateInputLayout);
    }

    private static long categoryToVehicleTypeId(String category) {
        if (category == null) return 1L;
        switch (category) {
            case "Luxury": return 2L;
            case "Van": return 3L;
            default: return 1L;
        }
    }

    private static String getText(com.google.android.material.textfield.TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private static void clearError(com.google.android.material.textfield.TextInputLayout layout) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
