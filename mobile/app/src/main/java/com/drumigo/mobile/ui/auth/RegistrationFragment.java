package com.drumigo.mobile.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.remote.ApiClient;
import com.drumigo.mobile.data.remote.dto.auth.request.PassengerRegisterRequest;
import com.drumigo.mobile.databinding.FragmentRegistrationBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationFragment extends Fragment {

    private FragmentRegistrationBinding binding;

    public RegistrationFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListeners();
    }

    private void setupClickListeners() {

        // Sign In link - Navigate back to Login
        binding.signInLink.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_registration_to_login)
        );

        // Upload Photo button - TODO: Implement photo picker in KT2
        binding.uploadPhotoButton.setOnClickListener(v -> {
            handlePhotoUpload();
        });

        // Create Account button - TODO: Implement registration in KT2
        binding.createAccountButton.setOnClickListener(v -> {
            handleRegistration();
        });

        // TODO: Prevent submission if terms of service not checked
    }

    private void handlePhotoUpload() {
        // TODO KT2: Implement photo picker
    }

    private void handleRegistration() {
        String firstName = getTextFromInput(binding.firstNameInput);
        String lastName = getTextFromInput(binding.lastNameInput);
        String email = getTextFromInput(binding.emailInput);
        String phone = getTextFromInput(binding.phoneInput);
        String address = getTextFromInput(binding.addressInput);
        String password = getTextFromInput(binding.passwordInput);
        String confirmPassword = getTextFromInput(binding.confirmPasswordInput);

        if (!validateInputs(firstName, lastName, email, phone, address, password, confirmPassword)) {
            return;
        }

        PassengerRegisterRequest request = new PassengerRegisterRequest(
                email,
                password,
                confirmPassword,
                firstName,
                lastName,
                address,
                phone,
                null
        );

        ApiClient.getAuthApi().registerPassenger(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_registration_to_login);
                } else {
                    String errorMsg = "HTTP " + response.code() + " - " + response.message();
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to reach server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs(
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            String password,
            String confirmPassword
    ) {
        boolean isValid = true;

        clearError(binding.firstNameInputLayout);
        clearError(binding.lastNameInputLayout);
        clearError(binding.emailInputLayout);
        clearError(binding.phoneInputLayout);
        clearError(binding.addressInputLayout);
        clearError(binding.passwordInputLayout);
        clearError(binding.confirmPasswordInputLayout);

        if (firstName.isEmpty()) {
            showError(binding.firstNameInputLayout, getString(R.string.error_first_name_required));
            isValid = false;
        }
        if (lastName.isEmpty()) {
            showError(binding.lastNameInputLayout, getString(R.string.error_last_name_required));
            isValid = false;
        }
        if (email.isEmpty()) {
            showError(binding.emailInputLayout, getString(R.string.error_email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(binding.emailInputLayout, getString(R.string.error_email_invalid));
            isValid = false;
        }
        if (phone.isEmpty()) {
            showError(binding.phoneInputLayout, getString(R.string.error_phone_required));
            isValid = false;
        }
        if (address.isEmpty()) {
            showError(binding.addressInputLayout, getString(R.string.error_address_required));
            isValid = false;
        }
        if (password.isEmpty()) {
            showError(binding.passwordInputLayout, getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < 6) {
            showError(binding.passwordInputLayout, getString(R.string.error_password_short));
            isValid = false;
        }
        if (!confirmPassword.isEmpty() && !confirmPassword.equals(password)) {
            showError(binding.confirmPasswordInputLayout, getString(R.string.error_passwords_not_match));
            isValid = false;
        } else if (confirmPassword.isEmpty()) {
            showError(binding.confirmPasswordInputLayout, getString(R.string.error_password_required));
            isValid = false;
        }

        return isValid;
    }

    private String getTextFromInput(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showError(com.google.android.material.textfield.TextInputLayout inputLayout,
                          String errorMessage) {
        inputLayout.setError(errorMessage);
        inputLayout.setErrorEnabled(true);
    }

    private void clearError(com.google.android.material.textfield.TextInputLayout inputLayout) {
        inputLayout.setError(null);
        inputLayout.setErrorEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
