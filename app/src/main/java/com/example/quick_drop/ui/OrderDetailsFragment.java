package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class OrderDetailsFragment extends Fragment {

    private TextView tvOrderID, tvPickupAddress, tvDestName, tvDestPhone, tvDropAddress, tvPartnerName, tvItemName, tvPaymentAmount, tvPaymentStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvOrderID = view.findViewById(R.id.tvOrderID);
        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvDestName = view.findViewById(R.id.tvDestName);
        tvDestPhone = view.findViewById(R.id.tvDestPhone);
        tvDropAddress = view.findViewById(R.id.tvDropAddress);
        tvPartnerName = view.findViewById(R.id.tvPartnerName);
        tvItemName = view.findViewById(R.id.tvItemName);
        tvPaymentAmount = view.findViewById(R.id.tvPaymentAmount);
        tvPaymentStatus = view.findViewById(R.id.tvPaymentStatus);

        String orderId = getArguments() != null ? getArguments().getString("order_id", "") : "";
        loadOrderDetails(orderId);

        MaterialButton btnFeedback = view.findViewById(R.id.btnLiveTrack);
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.feedbackFragment));
        }
    }

    private void loadOrderDetails(String orderId) {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (orderId != null && !orderId.isEmpty()) {
            ApiClient.getOrderDetails(token, orderId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject order) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> bindOrderData(order));
                }

                @Override
                public void onError(String error) {
                    loadLatestOrderFallback(token);
                }
            });
        } else {
            loadLatestOrderFallback(token);
        }
    }

    private void loadLatestOrderFallback(String token) {
        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONArray orders = response.optJSONArray("orders");
                    if (orders != null && orders.length() > 0) {
                        JSONObject order = orders.optJSONObject(0);
                        if (order != null) {
                            bindOrderData(order);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void bindOrderData(JSONObject order) {
        String orderId = order.optString("order_id", "");
        String shortId = orderId;
        if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
        if (tvOrderID != null) tvOrderID.setText("Order #" + shortId);

        if (tvPickupAddress != null) tvPickupAddress.setText(order.optString("pickup_address", ""));
        if (tvDestName != null) tvDestName.setText("Receiver");
        if (tvDestPhone != null) tvDestPhone.setText(order.optString("receiver_phone", ""));
        if (tvDropAddress != null) tvDropAddress.setText(order.optString("delivery_address", ""));

        String partner = order.optString("assigned_partner", "");
        if (tvPartnerName != null) {
            tvPartnerName.setText(partner.isEmpty() ? "Unassigned" : partner);
        }

        if (tvItemName != null) tvItemName.setText(order.optString("item_description", "Package"));

        double totalAmount = order.optDouble("total_amount", 0.0);
        if (tvPaymentAmount != null) {
            tvPaymentAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
        }

        String paymentStatus = order.optString("payment_status", "PENDING");
        if (tvPaymentStatus != null) {
            tvPaymentStatus.setText(paymentStatus);
        }
    }
}
