package com.example.quick_drop.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import org.json.JSONObject;

public class PartnerSignUpFragment extends Fragment {

    private boolean otpSent = false;
    private static final String MOCK_OTP = "1234";
    private boolean licenseUploaded = false;
    private TextView tvUploadStatus;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    licenseUploaded = true;
                    if (tvUploadStatus != null) {
                        tvUploadStatus.setText("License Photo Uploaded (OK)");
                    }
                    Toast.makeText(getContext(), "License Photo attached successfully", Toast.LENGTH_SHORT).show();
                }
            }
        }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        EditText etName = view.findViewById(R.id.etFullName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etAadhaar = view.findViewById(R.id.etAadhaar);
        EditText etOtp = view.findViewById(R.id.etOtp);

        if (etPhone != null) {
            etPhone.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(10) });
        }

        LinearLayout llOtpContainer = view.findViewById(R.id.llOtpContainer);
        View btnSendOtp = view.findViewById(R.id.btnSendOtp);
        tvUploadStatus = view.findViewById(R.id.tvUploadStatus);

        btnSendOtp.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Full name is required");
                return;
            }
            if (phone.isEmpty()) {
                etPhone.setError("Phone number is required");
                return;
            }
            if (phone.length() != 10) {
                etPhone.setError("Enter a valid 10-digit mobile number");
                return;
            }
            otpSent = true;
            llOtpContainer.setVisibility(View.VISIBLE);
            etOtp.setText(MOCK_OTP);
            Toast.makeText(getContext(), "OTP sent: " + MOCK_OTP, Toast.LENGTH_LONG).show();
        });

        view.findViewById(R.id.btnUploadID).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String aadhaar = etAadhaar.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Full name is required");
                return;
            }
            if (phone.isEmpty()) {
                etPhone.setError("Phone number is required");
                return;
            }
            if (phone.length() != 10) {
                etPhone.setError("Enter a valid 10-digit mobile number");
                return;
            }
            if (aadhaar.isEmpty()) {
                etAadhaar.setError("Aadhaar number is required");
                return;
            }
            if (!otpSent) {
                etPhone.setError("Please request OTP first");
                return;
            }
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty() || !otp.equals(MOCK_OTP)) {
                etOtp.setError("Please enter valid OTP (1234)");
                Toast.makeText(getContext(), "Invalid or expired OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!licenseUploaded) {
                Toast.makeText(getContext(), "Please upload License Photo", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiClient.signup(name, phone, "password123", "PARTNER", new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            String token = response.optString("token", "");
                            UserSession.registerUser(requireContext(), name, phone, "PARTNER", token);
                            Toast.makeText(getContext(), "Partner signed up successfully!", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).navigate(R.id.action_partnerSignup_to_partnerDashboard);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Backend Signup Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        });

        view.findViewById(R.id.tvSignIn).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_to_login));

        String[] vehicleTypes = new String[] {"Bicycle", "Motorcycle", "Car", "Van"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleTypes);
        AutoCompleteTextView actv = view.findViewById(R.id.actvVehicleType);
        actv.setAdapter(adapter);
    }
}
