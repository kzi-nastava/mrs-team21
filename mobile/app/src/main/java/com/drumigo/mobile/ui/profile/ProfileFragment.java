package com.drumigo.mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drumigo.mobile.MainActivity;
import com.drumigo.mobile.R;
import com.drumigo.mobile.databinding.FragmentProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Profile Fragment - Displays user profile information
 * Shows mock data for demonstration purposes
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    // Mock user data
    private static final String MOCK_FIRST_NAME = "John";
    private static final String MOCK_LAST_NAME = "Doe";
    private static final String MOCK_EMAIL = "john.doe@example.com";
    private static final String MOCK_PHONE = "+381 64 123 4567";
    private static final String MOCK_ADDRESS = "123 Main Street, Belgrade, Serbia";
    
    // Mock driver data
    private static final boolean IS_DRIVER = true;
    private static final String MOCK_VEHICLE_NAME = "Tesla Model 3";
    private static final String MOCK_VEHICLE_TYPE = "Luxury Sedan";
    private static final String MOCK_LICENSE_PLATE = "BG-123-AB";
    private static final int MOCK_SEATS = 4;
    private static final boolean HAS_BABY_SEATS = true;
    private static final boolean IS_PET_FRIENDLY = true;
    private static final int MOCK_ACTIVE_HOURS = 6;
    private static final int MOCK_ACTIVE_MINUTES = 45;
    private static final int MOCK_ACTIVE_PERCENTAGE = 84;

    public ProfileFragment() {
        // Required empty public constructor
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

        setupToolbar();
        populateMockData();
        setupClickListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });
    }

    private void populateMockData() {
        // Set user initials
        String initials = String.valueOf(MOCK_FIRST_NAME.charAt(0)) + MOCK_LAST_NAME.charAt(0);
        binding.avatarInitials.setText(initials);

        // Set user name
        String fullName = MOCK_FIRST_NAME + " " + MOCK_LAST_NAME;
        binding.userName.setText(fullName);

        // Set user role
        binding.userRole.setText(IS_DRIVER ? R.string.profile_driver : R.string.profile_passenger);

        // Personal information
        binding.firstName.setText(MOCK_FIRST_NAME);
        binding.lastName.setText(MOCK_LAST_NAME);
        binding.email.setText(MOCK_EMAIL);
        binding.phone.setText(MOCK_PHONE);
        binding.address.setText(MOCK_ADDRESS);

        // Driver-specific information
        if (IS_DRIVER) {
            // Show active hours section
            binding.activeHoursContainer.setVisibility(View.VISIBLE);
            String activeTime = MOCK_ACTIVE_HOURS + "h " + MOCK_ACTIVE_MINUTES + "m";
            binding.activeHoursValue.setText(activeTime);
            binding.activeHoursProgress.setProgress(MOCK_ACTIVE_PERCENTAGE);
            String progressText = MOCK_ACTIVE_PERCENTAGE + "% of daily limit (8 hours)";
            binding.activeHoursProgressText.setText(progressText);

            // Show vehicle information card
            binding.vehicleInfoCard.setVisibility(View.VISIBLE);
            binding.vehicleName.setText(MOCK_VEHICLE_NAME);
            binding.vehicleType.setText(MOCK_VEHICLE_TYPE);
            binding.licensePlate.setText(MOCK_LICENSE_PLATE);
            String seatsText = MOCK_SEATS + " passengers";
            binding.seatsCount.setText(seatsText);

            // Vehicle features
            binding.featureBabySeats.setVisibility(HAS_BABY_SEATS ? View.VISIBLE : View.GONE);
            binding.featurePetFriendly.setVisibility(IS_PET_FRIENDLY ? View.VISIBLE : View.GONE);
        } else {
            // Hide driver-specific sections for passengers
            binding.activeHoursContainer.setVisibility(View.GONE);
            binding.vehicleInfoCard.setVisibility(View.GONE);
        }

        // Hide pending changes notice by default (can be shown for demo)
        binding.pendingChangesCard.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        // Avatar edit badge - show photo options
        binding.avatarEditBadge.setOnClickListener(v -> showPhotoOptionsDialog());

        // Change password button
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Edit profile button
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Edit vehicle button (driver only)
        binding.btnEditVehicle.setOnClickListener(v -> showEditVehicleDialog());
    }

    private void showPhotoOptionsDialog() {
        // For demo purposes, just show a toast or simple dialog
        if (getContext() != null) {
            com.google.android.material.snackbar.Snackbar.make(
                binding.getRoot(),
                R.string.photo_updated,
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    private void showChangePasswordDialog() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        // Close button
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Cancel button
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelPassword);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        // Change password button - just dismiss for demo
        MaterialButton btnChange = dialogView.findViewById(R.id.btnChangePassword);
        if (btnChange != null) {
            btnChange.setOnClickListener(v -> {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.getRoot(),
                    "Password changed successfully",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(dialogView);

        // Pre-fill with mock data
        com.google.android.material.textfield.TextInputEditText editFirstName = 
            dialogView.findViewById(R.id.editFirstName);
        com.google.android.material.textfield.TextInputEditText editLastName = 
            dialogView.findViewById(R.id.editLastName);
        com.google.android.material.textfield.TextInputEditText editPhone = 
            dialogView.findViewById(R.id.editPhone);
        com.google.android.material.textfield.TextInputEditText editAddress = 
            dialogView.findViewById(R.id.editAddress);

        if (editFirstName != null) editFirstName.setText(MOCK_FIRST_NAME);
        if (editLastName != null) editLastName.setText(MOCK_LAST_NAME);
        if (editPhone != null) editPhone.setText(MOCK_PHONE);
        if (editAddress != null) editAddress.setText(MOCK_ADDRESS);

        // Close button
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Cancel button
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelEdit);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        // Save button
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveChanges);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (IS_DRIVER) {
                    // Show pending changes notice for drivers
                    binding.pendingChangesCard.setVisibility(View.VISIBLE);
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(),
                        R.string.profile_update_pending,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show();
                } else {
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(),
                        R.string.profile_updated,
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show();
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showEditVehicleDialog() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_vehicle, null);
        dialog.setContentView(dialogView);

        // Pre-fill with mock data
        com.google.android.material.textfield.TextInputEditText editVehicleName = 
            dialogView.findViewById(R.id.editVehicleName);
        com.google.android.material.textfield.TextInputEditText editLicensePlate = 
            dialogView.findViewById(R.id.editLicensePlate);
        com.google.android.material.checkbox.MaterialCheckBox checkBabySeats = 
            dialogView.findViewById(R.id.checkBabySeats);
        com.google.android.material.checkbox.MaterialCheckBox checkPetFriendly = 
            dialogView.findViewById(R.id.checkPetFriendly);

        if (editVehicleName != null) editVehicleName.setText(MOCK_VEHICLE_NAME);
        if (editLicensePlate != null) editLicensePlate.setText(MOCK_LICENSE_PLATE);
        if (checkBabySeats != null) checkBabySeats.setChecked(HAS_BABY_SEATS);
        if (checkPetFriendly != null) checkPetFriendly.setChecked(IS_PET_FRIENDLY);

        // Close button
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Cancel button
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelVehicle);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        // Save button - drivers always need admin approval
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveVehicle);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                binding.pendingChangesCard.setVisibility(View.VISIBLE);
                com.google.android.material.snackbar.Snackbar.make(
                    binding.getRoot(),
                    R.string.profile_update_pending,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
