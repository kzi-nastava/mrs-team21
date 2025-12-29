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
import com.drumigo.mobile.databinding.FragmentForgotPasswordBinding;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;

    public ForgotPasswordFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListeners();
    }


    private void setupClickListeners() {

        // Back button - Navigate back to Login
        binding.backButton.setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp()
        );

        // Sign In link - Navigate to Login
        binding.signInLink.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_forgot_password_to_login)
        );

        // Send Reset Link button
        binding.sendResetLinkButton.setOnClickListener(v -> {
            handleSendResetLink();
        });
    }

    private void handleSendResetLink() {
        String email = binding.emailInput.getText() != null ? 
                binding.emailInput.getText().toString().trim() : "";

        // TODO KT2: Implement actual password reset request
        if (!email.isEmpty()) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_forgot_password_to_reset);
        } else {
            binding.emailInputLayout.setError(getString(R.string.error_email_required));
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && 
               !email.isEmpty() && 
               android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void clearEmailError() {
        binding.emailInputLayout.setError(null);
        binding.emailInputLayout.setErrorEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
