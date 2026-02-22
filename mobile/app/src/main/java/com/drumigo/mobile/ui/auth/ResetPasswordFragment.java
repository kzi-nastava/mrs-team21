package com.drumigo.mobile.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.remote.ApiClient;
import com.drumigo.mobile.data.remote.dto.auth.request.SetPasswordRequest;
import com.drumigo.mobile.databinding.FragmentResetPasswordBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ResetPasswordFragment - Handles password reset with new password.
 * 
 * Features:
 * - New password input with visibility toggle
 * - Confirm password input with visibility toggle
 * - Real-time password requirements validation
 * - Visual feedback for each requirement
 * - Reset password button
 * 
 * Password Requirements:
 * - At least 8 characters
 * - One uppercase letter
 * - One lowercase letter
 * - One number or special character
 * 
 * UI Components:
 * - Logo header
 * - Shield icon
 * - Title and description
 * - Password inputs
 * - Requirements checklist with dynamic styling
 * - Reset button
 * - Security note
 * 
 * Note: For KT1, only UI and visual validation are implemented.
 * Backend integration will be added in KT2.
 * 
 * Specification Reference: 2.2.3
 */
public class ResetPasswordFragment extends Fragment {

    private FragmentResetPasswordBinding binding;

    public ResetPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupPasswordValidation();
        setupClickListeners();
    }

    /**
     * Sets up real-time password validation with visual feedback.
     */
    private void setupPasswordValidation() {
        binding.newPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordRequirements(s.toString());
            }
        });
    }

    /**
     * Validates password against all requirements and updates UI.
     * @param password The password to validate
     */
    private void validatePasswordRequirements(String password) {
        // Requirement 1: At least 8 characters
        boolean hasLength = password.length() >= 8;
        updateRequirementUI(
            binding.requirementLengthIcon,
            binding.requirementLengthText,
            hasLength
        );

        // Requirement 2: One uppercase letter
        boolean hasUppercase = password.matches(".*[A-Z].*");
        updateRequirementUI(
            binding.requirementUppercaseIcon,
            binding.requirementUppercaseText,
            hasUppercase
        );

        // Requirement 3: One lowercase letter
        boolean hasLowercase = password.matches(".*[a-z].*");
        updateRequirementUI(
            binding.requirementLowercaseIcon,
            binding.requirementLowercaseText,
            hasLowercase
        );

        // Requirement 4: One number or special character
        boolean hasSpecial = password.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        updateRequirementUI(
            binding.requirementSpecialIcon,
            binding.requirementSpecialText,
            hasSpecial
        );
    }

    private void updateRequirementUI(ImageView iconView, TextView textView, boolean isMet) {
        if (isMet) {
            iconView.setImageResource(R.drawable.ic_check);
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        } else {
            iconView.setImageResource(R.drawable.ic_circle);
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light));
        }
    }

    private void setupClickListeners() {
        binding.resetPasswordButton.setOnClickListener(v -> {
            handleResetPassword();
        });
    }

    private void handleResetPassword() {
        String newPassword = binding.newPasswordInput.getText() != null ?
                binding.newPasswordInput.getText().toString() : "";
        String confirmPassword = binding.confirmPasswordInput.getText() != null ?
                binding.confirmPasswordInput.getText().toString() : "";

        binding.newPasswordInputLayout.setError(null);
        binding.confirmPasswordInputLayout.setError(null);

        if (!isPasswordValid(newPassword)) {
            binding.newPasswordInputLayout.setError(getString(R.string.error_password_short));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.confirmPasswordInputLayout.setError(getString(R.string.error_passwords_not_match));
            return;
        }

        String activationToken = getArguments() != null ? getArguments().getString("activationToken") : null;
        if (activationToken != null && !activationToken.isEmpty()) {
            // Driver activation: set password via PUT /api/activation/{token}/set-password
            SetPasswordRequest request = new SetPasswordRequest(newPassword);
            ApiClient.getAuthApi().setDriverPassword(activationToken, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), R.string.reset_password_success_driver, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.action_reset_password_to_login);
                    } else {
                        String msg = getString(R.string.register_driver_failed);
                        if (response.errorBody() != null) {
                            try {
                                String body = response.errorBody().string();
                                if (body.contains("\"message\"")) {
                                    int q = body.indexOf("\"message\"");
                                    int c = body.indexOf(":", q);
                                    int q1 = body.indexOf("\"", c + 1);
                                    int q2 = body.indexOf("\"", q1 + 1);
                                    if (q1 > 0 && q2 > q1) msg = body.substring(q1 + 1, q2);
                                }
                            } catch (Exception ignored) { }
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), getString(R.string.register_driver_failed) + " " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            return;
        }

        // Forgot-password flow: TODO when backend integration is added
        Navigation.findNavController(requireView())
                .navigate(R.id.action_reset_password_to_login);
    }

    private boolean isPasswordValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        boolean hasLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasSpecial = password.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        return hasLength && hasUppercase && hasLowercase && hasSpecial;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
