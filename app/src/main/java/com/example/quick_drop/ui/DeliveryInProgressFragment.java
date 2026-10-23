package com.example.quick_drop.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
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

public class DeliveryInProgressFragment extends Fragment {

    private TextView tvOrderLabel, tvReceiverName, tvReceiverPhone;
    private MaterialButton btnNavigate, btnArrived;
    private String orderId = "";
    private double deliveryLat = 12.9352, deliveryLng = 77.6245;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_in_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvOrderLabel = view.findViewById(R.id.tvOrderLabel);
        tvReceiverName = view.findViewById(R.id.tvReceiverName);
        tvReceiverPhone = view.findViewById(R.id.tvReceiverPhone);
        btnNavigate = view.findViewById(R.id.btnNavigate);
        btnArrived = view.findViewById(R.id.btnArrived);

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
        }

        loadOrderDetails();

        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> launchGoogleMapsNavigation());
        }

        if (btnArrived != null) {
            btnArrived.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_inProgress_to_proofOfDelivery));
        }

        View btnCall = view.findViewById(R.id.btnCall);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                String phone = tvReceiverPhone != null ? tvReceiverPhone.getText().toString().trim() : "";
                if (!phone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            });
        }
    }

    private void loadOrderDetails() {
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
                            String shortId = orderId;
                            if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                            if (tvOrderLabel != null) tvOrderLabel.setText("Order #" + shortId);

                            deliveryLat = order.optDouble("delivery_lat", 12.9352);
                            deliveryLng = order.optDouble("delivery_lng", 77.6245);

                            if (tvReceiverPhone != null) {
                                tvReceiverPhone.setText(order.optString("receiver_phone", ""));
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void launchGoogleMapsNavigation() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + deliveryLat + "," + deliveryLng + "&mode=d");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + deliveryLat + "," + deliveryLng);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }
}
