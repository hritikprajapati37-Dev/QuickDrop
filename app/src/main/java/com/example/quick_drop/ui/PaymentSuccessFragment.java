package com.example.quick_drop.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import org.json.JSONObject;
import java.util.Locale;

public class PaymentSuccessFragment extends Fragment {

    private TextView tvOrderNumber, tvAmountPaid, tvPickupAddress, tvDeliveryAddress;
    private String orderId = "";
    private double bundleTotalAmount = 0.0;
    private String bundlePickup = "";
    private String bundleDelivery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Google Pay / Phone Pay style success checkmark bounce animation
        View checkCard = view.findViewById(R.id.cardCheckmark);
        if (checkCard != null) {
            checkCard.setScaleX(0f);
            checkCard.setScaleY(0f);
            checkCard.setRotation(-45f);
            checkCard.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .rotation(0f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();
        }

        tvOrderNumber = view.findViewById(R.id.tvOrderNumber);
        tvAmountPaid = view.findViewById(R.id.tvAmountPaid);
        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvDeliveryAddress = view.findViewById(R.id.tvDeliveryAddress);

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
            bundleTotalAmount = getArguments().getDouble("total_amount", 0.0);
            bundlePickup = getArguments().getString("pickup_address", "");
            bundleDelivery = getArguments().getString("delivery_address", "");
        }

        // Bind bundle data immediately so the user sees correct details without waiting for network or failing on HTTP 429
        bindDataToUi(orderId, bundleTotalAmount, bundlePickup, bundleDelivery);

        if (orderId.isEmpty()) {
            showOrderUnavailable();
        } else {
            loadOrderSuccessDetails();
        }

        view.findViewById(R.id.btnTrack).setOnClickListener(v -> {
            if (orderId.isEmpty()) {
                Toast.makeText(getContext(), "Order information is unavailable. Please return to the dashboard.", Toast.LENGTH_LONG).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("order_id", orderId);
            args.putDouble("total_amount", bundleTotalAmount);
            args.putString("pickup_address", bundlePickup);
            args.putString("delivery_address", bundleDelivery);
            Navigation.findNavController(v).navigate(R.id.action_paymentSuccess_to_tracking, args);
        });

        view.findViewById(R.id.btnDashboard).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_paymentSuccess_to_senderDashboard));
    }

    private void bindDataToUi(String id, double amount, String pickup, String delivery) {
        if (!id.isEmpty()) {
            String shortId = id;
            if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
            if (tvOrderNumber != null) tvOrderNumber.setText("#" + shortId);
        } else {
            if (tvOrderNumber != null) tvOrderNumber.setText("Order unavailable");
        }

        if (tvAmountPaid != null) {
            if (amount > 0.0) {
                tvAmountPaid.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
            } else {
                tvAmountPaid.setText("--");
            }
        }

        if (tvPickupAddress != null) {
            tvPickupAddress.setText(!pickup.isEmpty() ? pickup : "Pickup address not specified");
        }

        if (tvDeliveryAddress != null) {
            tvDeliveryAddress.setText(!delivery.isEmpty() ? delivery : "Delivery address not specified");
        }
    }

    private void loadOrderSuccessDetails() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty() || orderId.isEmpty()) {
            return;
        }

        ApiClient.getOrderDetails(token, orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject order) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (order != null) {
                        String fetchedOrderId = order.optString("order_id", orderId);
                        double totalAmount = order.optDouble("total_amount", bundleTotalAmount);
                        if (totalAmount <= 0.0) {
                            double dCharge = order.optDouble("delivery_charge", 0.0);
                            double pFee = order.optDouble("platform_fee", 2.0);
                            if (dCharge > 0.0) {
                                totalAmount = dCharge + pFee;
                            } else {
                                totalAmount = bundleTotalAmount;
                            }
                        }
                        String pickup = order.optString("pickup_address", bundlePickup);
                        String delivery = order.optString("delivery_address", bundleDelivery);

                        if (totalAmount > 0.0) bundleTotalAmount = totalAmount;
                        if (!pickup.isEmpty()) bundlePickup = pickup;
                        if (!delivery.isEmpty()) bundleDelivery = delivery;

                        bindDataToUi(fetchedOrderId, bundleTotalAmount, bundlePickup, bundleDelivery);
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Preserve bundle data on error / HTTP 429. Do NOT wipe UI with showOrderUnavailable()!
                Log.w("PaymentSuccessFragment", "getOrderDetails failed (e.g. rate limit HTTP 429): " + error + ". Using preserved bundle data.");
            }
        });
    }

    private void showOrderUnavailable() {
        if (tvOrderNumber != null) tvOrderNumber.setText("Order unavailable");
        if (tvAmountPaid != null) tvAmountPaid.setText("--");
        if (tvPickupAddress != null) tvPickupAddress.setText("Order details could not be loaded.");
        if (tvDeliveryAddress != null) tvDeliveryAddress.setText("");
    }
}
