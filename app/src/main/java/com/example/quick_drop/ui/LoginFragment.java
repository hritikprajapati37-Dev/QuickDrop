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

/**
 * Maps to Stitch Screen #6 (Login - Mobile OTP)
 */
public class LoginFragment extends Fragment {

    private boolean otpSent = false;
    private static final String MOCK_OTP = "1234";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etOtp = view.findViewById(R.id.etOtp);
        LinearLayout llOtpContainer = view.findViewById(R.id.llOtpContainer);
        View btnSendOtp = view.findViewById(R.id.btnSendOtp);

        btnSendOtp.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Please enter your mobile number");
                return;
            }
            if (phone.length() != 10) {
                etPhone.setError("Enter a valid 10-digit mobile number");
                return;
            }
            otpSent = true;
            llOtpContainer.setVisibility(View.VISIBLE);
            etOtp.setText(MOCK_OTP); // Auto-fill for convenience/demo
            Toast.makeText(getContext(), "OTP sent: " + MOCK_OTP, Toast.LENGTH_LONG).show();
        });

        view.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Please enter your mobile number");
                return;
            }
            if (phone.length() != 10) {
                etPhone.setError("Enter a valid 10-digit mobile number");
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

            ApiClient.login(phone, "password123", new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            String token = response.optString("token", "");
                            String role = response.optString("role", "SENDER");
                            String name = response.optString("name", "User");
                            UserSession.registerUser(requireContext(), name, phone, role, token);
                            Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();

                            if ("PARTNER".equals(role)) {
                                Navigation.findNavController(view).navigate(R.id.action_login_to_partnerDashboard);
                            } else {
                                Navigation.findNavController(view).navigate(R.id.action_login_to_senderDashboard);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Login Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        });

        view.findViewById(R.id.btnCreateAccount).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_login_to_selection));
    }
}
