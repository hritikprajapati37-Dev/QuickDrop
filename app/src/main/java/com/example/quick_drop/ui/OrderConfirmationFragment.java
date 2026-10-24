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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class OrderConfirmationFragment extends Fragment {

    private TextView tvPickupAddress, tvReceiverName, tvDeliveryAddress, tvItemDescription, tvDeliveryCharge, tvPlatformFee, tvTotalPayable;
    private String currentOrderId = "";
    private double bundleDeliveryCharge = 10.0;
    private double bundlePlatformFee = 2.0;
    private double bundleTotalAmount = 12.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_confirmation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvReceiverName = view.findViewById(R.id.tvReceiverName);
        tvDeliveryAddress = view.findViewById(R.id.tvDeliveryAddress);
        tvItemDescription = view.findViewById(R.id.tvItemDescription);
        tvDeliveryCharge = view.findViewById(R.id.tvDeliveryCharge);
        tvPlatformFee = view.findViewById(R.id.tvPlatformFee);
        tvTotalPayable = view.findViewById(R.id.tvTotalPayable);

        if (getArguments() != null) {
            currentOrderId = getArguments().getString("order_id", "");
            bundleDeliveryCharge = getArguments().getDouble("delivery_charge", 10.0);
            bundlePlatformFee = getArguments().getDouble("platform_fee", 2.0);
            bundleTotalAmount = getArguments().getDouble("total_amount", 12.0);
            String pAddr = getArguments().getString("pickup_address", "");
            String dAddr = getArguments().getString("delivery_address", "");
            String iDesc = getArguments().getString("item_description", "");

            if (!pAddr.isEmpty() && tvPickupAddress != null) tvPickupAddress.setText(pAddr);
            if (!dAddr.isEmpty() && tvDeliveryAddress != null) tvDeliveryAddress.setText(dAddr);
            if (!iDesc.isEmpty() && tvItemDescription != null) tvItemDescription.setText(iDesc);

            if (bundleDeliveryCharge <= 0.0) bundleDeliveryCharge = 10.0;
            if (bundlePlatformFee <= 0.0) bundlePlatformFee = 2.0;
            if (bundleTotalAmount <= 0.0) bundleTotalAmount = bundleDeliveryCharge + bundlePlatformFee;

            if (tvDeliveryCharge != null) tvDeliveryCharge.setText(String.format(Locale.getDefault(), "₹%.2f", bundleDeliveryCharge));
            if (tvPlatformFee != null) tvPlatformFee.setText(String.format(Locale.getDefault(), "₹%.2f", bundlePlatformFee));
            if (tvTotalPayable != null) tvTotalPayable.setText(String.format(Locale.getDefault(), "₹%.2f", bundleTotalAmount));
        }

        loadOrderDetails();

        view.findViewById(R.id.btnProceed).setOnClickListener(v -> {
            if (currentOrderId.isEmpty()) {
                Toast.makeText(getContext(), "Order ID missing. Please recreate delivery.", Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("order_id", currentOrderId);
            args.putDouble("delivery_charge", bundleDeliveryCharge);
            args.putDouble("platform_fee", bundlePlatformFee);
            args.putDouble("total_amount", bundleTotalAmount);
            Navigation.findNavController(v).navigate(R.id.action_orderConfirmation_to_payment, args);
        });
    }

    private void loadOrderDetails() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (!currentOrderId.isEmpty()) {
            ApiClient.getOrderDetails(token, currentOrderId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject order) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> bindOrder(order));
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
                            currentOrderId = order.optString("order_id", currentOrderId);
                            bindOrder(order);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private double parseOptDouble(JSONObject obj, String key, double defaultVal) {
        if (obj == null) return defaultVal;
        try {
            if (obj.has(key)) {
                String val = obj.getString(key);
                if (!val.isEmpty() && !val.equals("null")) {
                    return Double.parseDouble(val);
                }
            }
        } catch (Exception e) {
            try {
                return obj.optDouble(key, defaultVal);
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private void bindOrder(JSONObject order) {
        if (tvPickupAddress != null && (tvPickupAddress.getText() == null || tvPickupAddress.getText().toString().isEmpty())) {
            tvPickupAddress.setText(order.optString("pickup_address", "123 Main St"));
        }
        if (tvDeliveryAddress != null && (tvDeliveryAddress.getText() == null || tvDeliveryAddress.getText().toString().isEmpty())) {
            tvDeliveryAddress.setText(order.optString("delivery_address", "456 Market St"));
        }
        if (tvItemDescription != null && (tvItemDescription.getText() == null || tvItemDescription.getText().toString().isEmpty())) {
            tvItemDescription.setText(order.optString("item_description", "Standard Package"));
        }
        if (tvReceiverName != null) {
            tvReceiverName.setText("Phone: " + order.optString("receiver_phone", "9988776655"));
        }

        double dCharge = parseOptDouble(order, "delivery_charge", bundleDeliveryCharge);
        double pFee = parseOptDouble(order, "platform_fee", bundlePlatformFee);
        double tAmount = parseOptDouble(order, "total_amount", bundleTotalAmount);

        if (dCharge <= 0.0) {
            double dist = parseOptDouble(order, "distance_km", 2.0);
            if (dist <= 0.0) dist = 2.0;
            dCharge = Math.round(dist * 5.0 * 100.0) / 100.0;
        }
        if (pFee <= 0.0) pFee = 2.0;
        if (tAmount <= 0.0) {
            tAmount = Math.round((dCharge + pFee) * 100.0) / 100.0;
        }

        bundleDeliveryCharge = dCharge;
        bundlePlatformFee = pFee;
        bundleTotalAmount = tAmount;

        if (tvDeliveryCharge != null) {
            tvDeliveryCharge.setText(String.format(Locale.getDefault(), "₹%.2f", dCharge));
        }
        if (tvPlatformFee != null) {
            tvPlatformFee.setText(String.format(Locale.getDefault(), "₹%.2f", pFee));
        }
        if (tvTotalPayable != null) {
            tvTotalPayable.setText(String.format(Locale.getDefault(), "₹%.2f", tAmount));
        }
    }
}
