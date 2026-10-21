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

public class OrderConfirmationFragment extends Fragment {

    private TextView tvPickupAddress, tvReceiverName, tvDeliveryAddress, tvItemDescription, tvTotalPayable;

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
        tvTotalPayable = view.findViewById(R.id.tvTotalPayable);

        loadLatestOrder();

        view.findViewById(R.id.btnProceed).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_orderConfirmation_to_payment));
    }

    private void loadLatestOrder() {
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
                            if (tvPickupAddress != null) tvPickupAddress.setText(order.optString("pickup_address", ""));
                            if (tvReceiverName != null) tvReceiverName.setText("Phone: " + order.optString("receiver_phone", ""));
                            if (tvDeliveryAddress != null) tvDeliveryAddress.setText(order.optString("delivery_address", ""));
                            if (tvItemDescription != null) tvItemDescription.setText(order.optString("item_description", "Standard Package"));
                            if (tvTotalPayable != null) tvTotalPayable.setText("₹75");
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }
}
