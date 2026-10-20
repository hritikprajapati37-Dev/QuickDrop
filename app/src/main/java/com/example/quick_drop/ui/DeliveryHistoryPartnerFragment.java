package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;

public class DeliveryHistoryPartnerFragment extends Fragment {

    private LinearLayout llHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_history_partner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        llHistory = view.findViewById(R.id.llHistory);

        loadDeliveryHistory();

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.deliveryHistoryPartnerFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.partnerDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_history_to_partnerDashboard);
                    return true;
                } else if (id == R.id.availableRequestsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_history_to_availableRequests);
                    return true;
                } else if (id == R.id.earningsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_history_to_earnings);
                    return true;
                } else if (id == R.id.partnerProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_history_to_partnerProfile);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadDeliveryHistory() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (llHistory != null) {
                        llHistory.removeAllViews();
                        JSONArray orders = response.optJSONArray("orders");
                        if (orders != null && orders.length() > 0) {
                            for (int i = 0; i < orders.length(); i++) {
                                JSONObject order = orders.optJSONObject(i);
                                if (order != null) {
                                    View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_delivery_history, llHistory, false);
                                    
                                    TextView tvOrderNum = itemView.findViewById(R.id.tvOrderNum);
                                    TextView tvStatusText = itemView.findViewById(R.id.tvStatusText);
                                    TextView tvDateTime = itemView.findViewById(R.id.tvDateTime);
                                    TextView tvAddress = itemView.findViewById(R.id.tvAddress);
                                    TextView tvPackage = itemView.findViewById(R.id.tvPackage);
                                    View btnViewDetails = itemView.findViewById(R.id.btnViewDetails);

                                    String orderId = order.optString("order_id", "");
                                    String shortId = orderId;
                                    if (shortId.length() > 6) {
                                        shortId = shortId.substring(shortId.length() - 6);
                                    }
                                    if (tvOrderNum != null) tvOrderNum.setText("Order #" + shortId);
                                    
                                    boolean delivered = order.optBoolean("delivered", false);
                                    String status = order.optString("status", "CREATED");
                                    if (tvStatusText != null) {
                                        if (delivered || "DELIVERED".equals(status)) {
                                            tvStatusText.setText("Delivered");
                                        } else {
                                            tvStatusText.setText(status);
                                        }
                                    }

                                    if (tvDateTime != null) tvDateTime.setText("Recent");
                                    if (tvAddress != null) tvAddress.setText(order.optString("delivery_address", ""));
                                    if (tvPackage != null) tvPackage.setText(order.optString("item_description", "Standard Package"));

                                    if (btnViewDetails != null) {
                                        btnViewDetails.setOnClickListener(v -> {
                                            Bundle args = new Bundle();
                                            args.putString("order_id", orderId);
                                            Navigation.findNavController(v).navigate(R.id.action_history_to_details, args);
                                        });
                                    }

                                    llHistory.addView(itemView);
                                }
                            }
                        } else {
                            TextView emptyTv = new TextView(requireContext());
                            emptyTv.setText("No delivery history available yet.");
                            emptyTv.setPadding(32, 64, 32, 64);
                            emptyTv.setGravity(android.view.Gravity.CENTER);
                            llHistory.addView(emptyTv);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (llHistory != null) {
                        llHistory.removeAllViews();
                        TextView errorTv = new TextView(requireContext());
                        errorTv.setText("Failed to load delivery history. Tap to retry.");
                        errorTv.setPadding(32, 64, 32, 64);
                        errorTv.setGravity(android.view.Gravity.CENTER);
                        errorTv.setOnClickListener(v -> loadDeliveryHistory());
                        llHistory.addView(errorTv);
                    }
                });
            }
        });
    }
}
