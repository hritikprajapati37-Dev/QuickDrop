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

    private TextView tvOrderID, tvPickupAddress, tvDestName, tvDestPhone, tvDropAddress, tvPartnerName, tvItemType, tvItemWeight, tvPaymentAmount, tvPaymentStatus;
    private TextView tvTime1, tvTime2, tvTime3;

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
        tvItemType = view.findViewById(R.id.tvItemType);
        tvItemWeight = view.findViewById(R.id.tvItemWeight);
        tvPaymentAmount = view.findViewById(R.id.tvPaymentAmount);
        tvPaymentStatus = view.findViewById(R.id.tvPaymentStatus);

        tvTime1 = view.findViewById(R.id.tvTime1);
        tvTime2 = view.findViewById(R.id.tvTime2);
        tvTime3 = view.findViewById(R.id.tvTime3);

        String orderId = getArguments() != null ? getArguments().getString("order_id", "") : "";
        loadOrderDetails(orderId);

        MaterialButton btnFeedback = view.findViewById(R.id.btnGiveFeedback);
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
        String sId = orderId;
        if (sId.length() > 6) sId = sId.substring(sId.length() - 6);
        if (tvOrderID != null) tvOrderID.setText("Order #" + sId);

        if (tvPickupAddress != null) tvPickupAddress.setText(order.optString("pickup_address", ""));
        if (tvDestName != null) tvDestName.setText("Receiver");
        if (tvDestPhone != null) tvDestPhone.setText(order.optString("receiver_phone", ""));
        if (tvDropAddress != null) tvDropAddress.setText(order.optString("delivery_address", ""));

        String partner = order.optString("assigned_partner", "");
        if (tvPartnerName != null) {
            tvPartnerName.setText(partner == null || partner.isEmpty() || "null".equals(partner) ? "Unassigned Partner" : partner);
        }

        if (tvItemType != null) tvItemType.setText(order.optString("item_description", "Package"));
        if (tvItemWeight != null) tvItemWeight.setText(order.optString("item_weight", "1.0 kg"));

        double totalAmount = order.optDouble("total_amount", 0.0);
        if (tvPaymentAmount != null) {
            tvPaymentAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
        }

        String paymentStatus = order.optString("payment_status", "PENDING");
        if (tvPaymentStatus != null) {
            tvPaymentStatus.setText(paymentStatus);
        }

        // Bind exact stored database timestamps
        String orderTime = order.optString("order_time", order.optString("created_at", ""));
        String pickupTime = order.optString("pickup_time", "");
        String completeTime = order.optString("complete_order_time", "");

        if (tvTime1 != null) tvTime1.setText(formatStoredTime(orderTime));
        if (tvTime2 != null) tvTime2.setText(formatStoredTime(pickupTime));
        if (tvTime3 != null) tvTime3.setText(formatStoredTime(completeTime));
    }

    private String formatStoredTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty() || "null".equals(isoTime)) {
            return "Pending";
        }
        try {
            String cleanIso = isoTime.replace("Z", "");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            java.util.Date date;
            try {
                date = sdf.parse(cleanIso);
            } catch (Exception e) {
                java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                date = sdf2.parse(cleanIso);
            }
            if (date != null) {
                java.text.SimpleDateFormat timeSdf = new java.text.SimpleDateFormat("h:mm a", Locale.getDefault());
                return timeSdf.format(date);
            }
        } catch (Exception ignored) {}
        return isoTime;
    }
}
