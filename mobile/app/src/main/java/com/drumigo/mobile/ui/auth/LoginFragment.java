package com.drumigo.mobile.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.drumigo.mobile.data.remote.dto.auth.request.LoginRequest;
import com.drumigo.mobile.data.remote.dto.auth.response.LoginResponse;
import com.drumigo.mobile.databinding.FragmentLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    public LoginFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListeners();
    }

    private void setupClickListeners() {

        // Sign Up link - Navigate to Registration
        binding.signUpLink.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_login_to_registration)
        );

        // Forgot Password link - Navigate to Forgot Password
        binding.forgotPasswordLink.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_login_to_forgot_password)
        );

        // Sign In button - TODO: Implement authentication in KT2
        binding.signInButton.setOnClickListener(v -> {
            handleSignIn();
        });

        // Guest button - TODO: Navigate to ride estimation in KT2
        binding.guestButton.setOnClickListener(v -> {
            handleGuestAccess();
        });

        // TODO: Save preference to SharedPreferences
    }

    private void handleSignIn() {
        String email = binding.emailInput.getText() != null ? 
                binding.emailInput.getText().toString().trim() : "";
        String password = binding.passwordInput.getText() != null ? 
                binding.passwordInput.getText().toString() : "";
        boolean rememberMe = binding.rememberMeCheckbox.isChecked();

        if (!validateInputs(email, password)) {
            return;
        }

        LoginRequest request = new LoginRequest(email, password);

        ApiClient.getAuthApi().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    // Store authentication data
                    SharedPreferences prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString("token", loginResponse.token)
                            .putLong("userId", loginResponse.userId)
                            .putString("email", loginResponse.email)
                            .putString("role", loginResponse.role)
                            .apply();

                    Toast.makeText(requireContext(), "Welcome back, " + loginResponse.email + "!", Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to main app screen
                } else {
                    String errorMsg = "HTTP " + response.code() + " - " + response.message();
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to reach server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        clearError(binding.emailInputLayout);
        clearError(binding.passwordInputLayout);

        if (email.isEmpty()) {
            showError(binding.emailInputLayout, getString(R.string.error_email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(binding.emailInputLayout, getString(R.string.error_email_invalid));
            isValid = false;
        }

        if (password.isEmpty()) {
            showError(binding.passwordInputLayout, getString(R.string.error_password_required));
            isValid = false;
        }

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


    private void handleGuestAccess() {
        // TODO KT2: Navigate to ride estimation screen
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
