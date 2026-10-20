package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import org.json.JSONObject;

public class SenderSignUpFragment extends Fragment {

    private boolean otpSent = false;
    private static final String MOCK_OTP = "1234";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sender_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        EditText etName = view.findViewById(R.id.etFullName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etOtp = view.findViewById(R.id.etOtp);
        LinearLayout llOtpContainer = view.findViewById(R.id.llOtpContainer);
        View btnSendOtp = view.findViewById(R.id.btnSendOtp);

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
            otpSent = true;
            llOtpContainer.setVisibility(View.VISIBLE);
            etOtp.setText(MOCK_OTP);
            Toast.makeText(getContext(), "OTP sent: " + MOCK_OTP, Toast.LENGTH_LONG).show();
        });

        view.findViewById(R.id.btnSignUp).setOnClickListener(v -> {
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

            ApiClient.signup(name, phone, "password123", "SENDER", new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            String token = response.optString("token", "");
                            UserSession.registerUser(requireContext(), name, phone, "SENDER", token);
                            Toast.makeText(getContext(), "Signed up successfully!", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).navigate(R.id.action_senderSignup_to_senderDashboard);
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

        view.findViewById(R.id.btnLoginLink).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_to_login));
    }
}
