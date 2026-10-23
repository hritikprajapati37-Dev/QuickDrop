package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;

public class PickupConfirmationFragment extends Fragment {

    private TextView tvPickupAddress;
    private EditText etPickupOtp;
    private MaterialButton btnConfirmPickup;
    private String orderId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pickup_confirmation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        etPickupOtp = view.findViewById(R.id.etPickupOtp);
        btnConfirmPickup = view.findViewById(R.id.btnConfirmPickup);

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
        }

        loadOrderData();

        if (btnConfirmPickup != null) {
            btnConfirmPickup.setOnClickListener(this::confirmPickup);
        }
    }

    private void loadOrderData() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONArray orders = response.optJSONArray("orders");
                    if (orders != null && orders.length() > 0) {
                        JSONObject order = orders.optJSONObject(0);
                        if (order != null) {
                            if (orderId.isEmpty()) {
                                orderId = order.optString("order_id", "");
                            }
                            if (tvPickupAddress != null) {
                                tvPickupAddress.setText(order.optString("pickup_address", ""));
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void confirmPickup(View v) {
        String otp = etPickupOtp != null && etPickupOtp.getText() != null ? etPickupOtp.getText().toString().trim() : "";
        if (otp.isEmpty()) {
            if (etPickupOtp != null) etPickupOtp.setError("Please enter the pickup OTP");
            return;
        }

        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (btnConfirmPickup != null) btnConfirmPickup.setEnabled(false);

        ApiClient.verifyPickup(token, orderId, otp, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Pickup verified! Navigating to drop-off.", Toast.LENGTH_SHORT).show();
                    try {
                        Navigation.findNavController(v).navigate(R.id.action_pickupConfirmation_to_deliveryInProgress);
                    } catch (Exception e) {
                        Navigation.findNavController(v).navigateUp();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (btnConfirmPickup != null) btnConfirmPickup.setEnabled(true);
                    Toast.makeText(getContext(), "Verification failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
