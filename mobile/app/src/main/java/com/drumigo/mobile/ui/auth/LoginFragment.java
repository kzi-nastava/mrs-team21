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
import com.drumigo.mobile.databinding.FragmentLoginBinding;

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
