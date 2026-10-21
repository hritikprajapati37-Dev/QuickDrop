package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Locale;

public class PaymentFragment extends Fragment {

    private TextView tvOrderNumber, tvDeliveryCharge, tvPlatformFee, tvTotalPayable;
    private MaterialButton btnPayNow;
    private String currentOrderId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).popBackStack());

        tvOrderNumber = view.findViewById(R.id.tvOrderNumber);
        tvDeliveryCharge = view.findViewById(R.id.tvDeliveryCharge);
        tvPlatformFee = view.findViewById(R.id.tvPlatformFee);
        tvTotalPayable = view.findViewById(R.id.tvTotalPayable);
        btnPayNow = view.findViewById(R.id.btnPayNow);

        loadLatestOrderPayment();

        if (btnPayNow != null) {
            btnPayNow.setOnClickListener(v -> processPayment(v));
        }
    }

    private void loadLatestOrderPayment() {
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
                            currentOrderId = order.optString("order_id", "");
                            String shortId = currentOrderId;
                            if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                            if (tvOrderNumber != null) tvOrderNumber.setText("#" + shortId);

                            double deliveryCharge = order.optDouble("delivery_charge", 0.0);
                            double platformFee = order.optDouble("platform_fee", 2.0);
                            double totalAmount = order.optDouble("total_amount", deliveryCharge + platformFee);

                            if (tvDeliveryCharge != null) {
                                tvDeliveryCharge.setText(String.format(Locale.getDefault(), "₹%.2f", deliveryCharge));
                            }
                            if (tvPlatformFee != null) {
                                tvPlatformFee.setText(String.format(Locale.getDefault(), "₹%.2f", platformFee));
                            }
                            if (tvTotalPayable != null) {
                                tvTotalPayable.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
                            }
                            if (btnPayNow != null) {
                                btnPayNow.setText(String.format(Locale.getDefault(), "Pay ₹%.2f →", totalAmount));
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void processPayment(View v) {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty() || currentOrderId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid order or session", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnPayNow != null) {
            btnPayNow.setEnabled(false);
            btnPayNow.setText("Processing Payment...");
        }

        ApiClient.payOrder(token, currentOrderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Payment confirmed & saved to database!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.action_paymentFragment_to_paymentSuccess);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (btnPayNow != null) {
                        btnPayNow.setEnabled(true);
                        btnPayNow.setText("Retry Payment");
                    }
                    Toast.makeText(getContext(), "Payment failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
