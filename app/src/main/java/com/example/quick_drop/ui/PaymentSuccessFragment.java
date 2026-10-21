package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

public class PaymentSuccessFragment extends Fragment {

    private TextView tvOrderNumber, tvAmountPaid, tvPickupAddress, tvDeliveryAddress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOrderNumber = view.findViewById(R.id.tvOrderNumber);
        tvAmountPaid = view.findViewById(R.id.tvAmountPaid);
        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvDeliveryAddress = view.findViewById(R.id.tvDeliveryAddress);

        loadOrderSuccessDetails();

        view.findViewById(R.id.btnTrack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_paymentSuccess_to_tracking));

        view.findViewById(R.id.btnDashboard).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_paymentSuccess_to_senderDashboard));
    }

    private void loadOrderSuccessDetails() {
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
                            String orderId = order.optString("order_id", "");
                            String shortId = orderId;
                            if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                            if (tvOrderNumber != null) tvOrderNumber.setText("#" + shortId);

                            double totalAmount = order.optDouble("total_amount", 0.0);
                            if (tvAmountPaid != null) {
                                tvAmountPaid.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
                            }
                            if (tvPickupAddress != null) tvPickupAddress.setText(order.optString("pickup_address", ""));
                            if (tvDeliveryAddress != null) tvDeliveryAddress.setText(order.optString("delivery_address", ""));
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }
}
