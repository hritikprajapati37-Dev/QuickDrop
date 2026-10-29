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
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DeliveryCompletedFragment extends Fragment {

    private TextView tvOrderId, tvDeliveredTime, tvReceiverName, tvDestination, tvPackage;
    private String orderId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_completed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOrderId = view.findViewById(R.id.tvOrderId);
        tvDeliveredTime = view.findViewById(R.id.tvDeliveredTime);
        tvReceiverName = view.findViewById(R.id.tvReceiverName);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvPackage = view.findViewById(R.id.tvPackage);

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
        }

        if (!orderId.isEmpty()) {
            loadOrderDetails();
        } else {
            if (tvOrderId != null) tvOrderId.setText("#QD-0000");
        }

        view.findViewById(R.id.btnDashboard).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_completed_to_dashboard));

        view.findViewById(R.id.btnEarnings).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_completed_to_earnings));

        View successArea = view.findViewById(R.id.successArea);
        if (successArea != null) {
            successArea.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("order_id", orderId);
                Navigation.findNavController(v).navigate(R.id.action_completed_to_feedback, args);
            });
        }
            
        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_completed_to_dashboard));
    }

    private void loadOrderDetails() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getOrderDetails(token, orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject order) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (order != null) {
                        String shortId = orderId;
                        if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                        if (tvOrderId != null) tvOrderId.setText("#" + shortId);

                        String completeTime = order.optString("complete_order_time", "");
                        if (tvDeliveredTime != null) {
                            if (!completeTime.isEmpty()) {
                                tvDeliveredTime.setText(formatTime(completeTime));
                            } else {
                                tvDeliveredTime.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));
                            }
                        }

                        String receiverPhone = order.optString("receiver_phone", "");
                        if (tvReceiverName != null) {
                            tvReceiverName.setText(!receiverPhone.isEmpty() ? "Receiver (" + receiverPhone + ")" : "Receiver");
                        }

                        String deliveryAddress = order.optString("delivery_address", "");
                        if (tvDestination != null) {
                            tvDestination.setText(!deliveryAddress.isEmpty() ? deliveryAddress : "Destination address not specified");
                        }

                        String itemDesc = order.optString("item_description", "Package");
                        String itemWeight = order.optString("item_weight", "");
                        if (tvPackage != null) {
                            tvPackage.setText(itemWeight.isEmpty() ? itemDesc : itemDesc + " (" + itemWeight + ")");
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Failed to load order details: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String formatTime(String isoTime) {
        try {
            String cleanIso = isoTime.replace("Z", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(cleanIso);
            if (date != null) {
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
            }
        } catch (Exception ignored) {}
        try {
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf2.parse(isoTime);
            if (date != null) {
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
            }
        } catch (Exception ignored) {}
        return "Delivered";
    }
}
