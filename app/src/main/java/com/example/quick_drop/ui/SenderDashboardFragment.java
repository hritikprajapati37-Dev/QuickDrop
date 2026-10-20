package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.quick_drop.utils.UserSession;
import android.widget.TextView;
import org.json.JSONObject;

public class SenderDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sender_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvOrderID = view.findViewById(R.id.tvOrderID);
        TextView tvPickupVal = view.findViewById(R.id.tvPickupVal);
        TextView tvDestVal = view.findViewById(R.id.tvDestVal);
        TextView tvActiveCount = view.findViewById(R.id.tvActiveCount);
        TextView tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        TextView tvPendingCount = view.findViewById(R.id.tvPendingCount);
        View cardActiveOrder = view.findViewById(R.id.cardActiveOrder);

        if (tvGreeting != null) {
            tvGreeting.setText("Good Morning, " + UserSession.getUserName(requireContext()).split(" ")[0]);
        }

        String token = UserSession.getToken(requireContext());
        if (!token.isEmpty()) {
            ApiClient.getDashboard(token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            String name = response.optString("name", "User");
                            if (tvGreeting != null) {
                                tvGreeting.setText("Good Morning, " + name.split(" ")[0]);
                            }

                            JSONObject counts = response.optJSONObject("counts");
                            if (counts != null) {
                                if (tvActiveCount != null) tvActiveCount.setText(String.valueOf(counts.optInt("active", 0)));
                                if (tvCompletedCount != null) tvCompletedCount.setText(String.valueOf(counts.optInt("completed", 0)));
                                if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(counts.optInt("pending", 0)));
                            }

                            JSONObject activeDelivery = response.optJSONObject("active_delivery");
                            if (activeDelivery != null) {
                                if (cardActiveOrder != null) cardActiveOrder.setVisibility(View.VISIBLE);
                                if (tvOrderID != null) {
                                    String oid = activeDelivery.optString("order_id", "");
                                    if (oid.length() > 6) {
                                        oid = oid.substring(oid.length() - 6);
                                    }
                                    tvOrderID.setText("#" + oid);
                                }
                                if (tvPickupVal != null) tvPickupVal.setText(activeDelivery.optString("pickup_address", ""));
                                if (tvDestVal != null) tvDestVal.setText(activeDelivery.optString("delivery_address", ""));
                            } else {
                                if (cardActiveOrder != null) cardActiveOrder.setVisibility(View.GONE);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (cardActiveOrder != null) cardActiveOrder.setVisibility(View.GONE);
                            if (tvActiveCount != null) tvActiveCount.setText("0");
                            if (tvCompletedCount != null) tvCompletedCount.setText("0");
                            if (tvPendingCount != null) tvPendingCount.setText("0");
                        });
                    }
                }
            });
        } else {
            if (cardActiveOrder != null) cardActiveOrder.setVisibility(View.GONE);
            if (tvActiveCount != null) tvActiveCount.setText("0");
            if (tvCompletedCount != null) tvCompletedCount.setText("0");
            if (tvPendingCount != null) tvPendingCount.setText("0");
        }

        view.findViewById(R.id.btnCreateDelivery).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_createDelivery));

        view.findViewById(R.id.ivProfile).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_profile));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.senderDashboardFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.deliveriesFragment) {
                Navigation.findNavController(view).navigate(R.id.action_senderDashboard_to_deliveries);
                return true;
            } else if (id == R.id.orderHistorySenderFragment) {
                Navigation.findNavController(view).navigate(R.id.action_senderDashboard_to_orderHistory);
                return true;
            } else if (id == R.id.senderProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_senderDashboard_to_profile);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnViewActiveDetails).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_tracking));
            
        view.findViewById(R.id.cardActiveOrder).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_orderDetails));

        view.findViewById(R.id.btnViewAllOrders).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_orderHistory));
    }
}
