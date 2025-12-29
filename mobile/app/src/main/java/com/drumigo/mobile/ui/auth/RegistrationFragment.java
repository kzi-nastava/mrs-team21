package com.drumigo.mobile.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.drumigo.mobile.MainActivity;
import com.drumigo.mobile.R;
import com.drumigo.mobile.databinding.FragmentRegistrationBinding;

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
        // Get all input values
        String firstName = getTextFromInput(binding.firstNameInput);
        String lastName = getTextFromInput(binding.lastNameInput);
        String email = getTextFromInput(binding.emailInput);
        String countryCode = getTextFromInput(binding.countryCodeInput);
        String phone = getTextFromInput(binding.phoneInput);
        String address = getTextFromInput(binding.addressInput);
        String password = getTextFromInput(binding.passwordInput);
        String confirmPassword = getTextFromInput(binding.confirmPasswordInput);
        boolean termsAccepted = binding.termsCheckbox.isChecked();

        // TODO: Implement validation and registration
    }

    private String getTextFromInput(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // TODO KT2: Implement validation for each field

        return isValid;
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
