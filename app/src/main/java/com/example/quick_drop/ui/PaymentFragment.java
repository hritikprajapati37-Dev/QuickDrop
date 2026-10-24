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
import com.google.android.material.radiobutton.MaterialRadioButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class PaymentFragment extends Fragment {

    private TextView tvOrderNumber, tvDeliveryCharge, tvPlatformFee, tvTotalPayable;
    private MaterialButton btnPayNow;
    private MaterialRadioButton rbUpi, rbCard, rbNetBanking;
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

        rbUpi = view.findViewById(R.id.rbUpi);
        rbCard = view.findViewById(R.id.rbCard);
        rbNetBanking = view.findViewById(R.id.rbNetBanking);

        View.OnClickListener paymentMethodListener = v -> {
            boolean isUpi = (v == rbUpi || (rbUpi != null && v == rbUpi.getParent()));
            boolean isCard = (v == rbCard || (rbCard != null && v == rbCard.getParent()));
            boolean isNetBanking = (v == rbNetBanking || (rbNetBanking != null && v == rbNetBanking.getParent()));

            if (rbUpi != null) rbUpi.setChecked(isUpi);
            if (rbCard != null) rbCard.setChecked(isCard);
            if (rbNetBanking != null) rbNetBanking.setChecked(isNetBanking);
        };

        if (rbUpi != null) {
            rbUpi.setChecked(true); // Default to UPI
            rbUpi.setOnClickListener(paymentMethodListener);
            if (rbUpi.getParent() instanceof View) {
                ((View) rbUpi.getParent()).setOnClickListener(paymentMethodListener);
            }
        }
        if (rbCard != null) {
            rbCard.setOnClickListener(paymentMethodListener);
            if (rbCard.getParent() instanceof View) {
                ((View) rbCard.getParent()).setOnClickListener(paymentMethodListener);
            }
        }
        if (rbNetBanking != null) {
            rbNetBanking.setOnClickListener(paymentMethodListener);
            if (rbNetBanking.getParent() instanceof View) {
                ((View) rbNetBanking.getParent()).setOnClickListener(paymentMethodListener);
            }
        }

        if (getArguments() != null) {
            currentOrderId = getArguments().getString("order_id", "");
        }

        loadOrderPaymentDetails();

        if (btnPayNow != null) {
            btnPayNow.setOnClickListener(v -> processPayment(v));
        }
    }

    private void loadOrderPaymentDetails() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (!currentOrderId.isEmpty()) {
            ApiClient.getOrderDetails(token, currentOrderId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject order) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> bindPaymentDetails(order));
                }

                @Override
                public void onError(String error) {
                    loadLatestOrderPaymentFallback(token);
                }
            });
        } else {
            loadLatestOrderPaymentFallback(token);
        }
    }

    private void loadLatestOrderPaymentFallback(String token) {
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
                            bindPaymentDetails(order);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void bindPaymentDetails(JSONObject order) {
        String shortId = currentOrderId;
        if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
        if (tvOrderNumber != null) tvOrderNumber.setText("#" + shortId);

        double deliveryCharge = order.optDouble("delivery_charge", 0.0);
        double platformFee = order.optDouble("platform_fee", 2.0);
        double totalAmount = order.optDouble("total_amount", deliveryCharge + platformFee);

        if (deliveryCharge <= 0.0) {
            double dist = order.optDouble("distance_km", 1.0);
            deliveryCharge = Math.round(dist * 5.0 * 100.0) / 100.0;
            totalAmount = Math.round((deliveryCharge + platformFee) * 100.0) / 100.0;
        }

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
                        btnPayNow.setText("Pay Now");
                    }
                    Toast.makeText(getContext(), "Payment failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
