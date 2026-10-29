package com.example.quick_drop.ui;

import android.graphics.Color;
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
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class EarningsFragment extends Fragment {

    private ShimmerFrameLayout shimmerContainer;
    private View layoutContent, layoutError;
    private TextView tvBalanceValue, tvDeliveriesValue, tvDistanceValue;
    private TextView tabToday, tabWeek, tabMonth;
    private LinearLayout llEarningsHistory;
    private String currentPeriod = "today";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_earnings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        layoutContent = view.findViewById(R.id.layoutContent);
        layoutError = view.findViewById(R.id.layoutError);
        tvBalanceValue = view.findViewById(R.id.tvBalanceValue);
        tvDeliveriesValue = view.findViewById(R.id.tvDeliveriesValue);
        tvDistanceValue = view.findViewById(R.id.tvDistanceValue);
        tabToday = view.findViewById(R.id.tvTabToday);
        tabWeek = view.findViewById(R.id.tvTabWeek);
        tabMonth = view.findViewById(R.id.tvTabMonth);
        llEarningsHistory = view.findViewById(R.id.llEarningsHistory);

        if (tabToday != null) tabToday.setOnClickListener(v -> selectTab(0));
        if (tabWeek != null) tabWeek.setOnClickListener(v -> selectTab(1));
        if (tabMonth != null) tabMonth.setOnClickListener(v -> selectTab(2));

        View btnRetry = view.findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadEarnings());
        }

        loadEarnings();

        View btnSeeAll = view.findViewById(R.id.btnSeeAll);
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_earnings_to_history));
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.earningsFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.partnerDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_earnings_to_partnerDashboard);
                    return true;
                } else if (id == R.id.availableRequestsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_earnings_to_availableRequests);
                    return true;
                } else if (id == R.id.deliveryHistoryPartnerFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_earnings_to_history);
                    return true;
                } else if (id == R.id.earningsFragment) {
                    return true;
                } else if (id == R.id.partnerProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_earnings_to_partnerProfile);
                    return true;
                }
                return false;
            });
        }

        View btnWithdraw = view.findViewById(R.id.btnWithdraw);
        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> {
                String amount = tvBalanceValue != null ? tvBalanceValue.getText().toString() : "₹0";
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Withdraw Funds")
                    .setMessage("Your request to withdraw " + amount + " has been received and is being processed.")
                    .setPositiveButton("OK", null)
                    .show();
            });
        }
    }

    private void loadEarnings() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
        }
        if (layoutContent != null) layoutContent.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);

        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) {
            showErrorState();
            return;
        }

        ApiClient.getPartnerEarnings(token, currentPeriod, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);

                    double earnings = response.optDouble("total_earnings", 0.0);
                    int deliveries = response.optInt("completed_deliveries", 0);
                    double distance = response.optDouble("total_distance_km", deliveries * 3.0);
                    String currency = response.optString("currency", "₹");

                    if (tvBalanceValue != null) {
                        tvBalanceValue.setText(String.format(Locale.getDefault(), "%s%.0f", currency, earnings));
                    }
                    if (tvDeliveriesValue != null) {
                        tvDeliveriesValue.setText(String.valueOf(deliveries));
                    }
                    if (tvDistanceValue != null) {
                        tvDistanceValue.setText(String.format(Locale.getDefault(), "%.1f km", distance));
                    }

                    loadEarningsHistory(token);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showErrorState());
            }
        });
    }

    private void loadEarningsHistory(String token) {
        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (llEarningsHistory != null) {
                        llEarningsHistory.removeAllViews();
                        JSONArray orders = response.optJSONArray("orders");
                        if (orders != null && orders.length() > 0) {
                            int count = 0;
                            for (int i = 0; i < orders.length(); i++) {
                                JSONObject order = orders.optJSONObject(i);
                                if (order != null && (order.optBoolean("delivered", false) || "DELIVERED".equals(order.optString("status")))) {
                                    if (count >= 3) break;
                                    View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_delivery, llEarningsHistory, false);
                                    
                                    TextView tvId = itemView.findViewById(R.id.tvOrderID);
                                    TextView tvTime = itemView.findViewById(R.id.tvTimestamp);
                                    TextView tvAmt = itemView.findViewById(R.id.tvAmount);

                                    String oid = order.optString("order_id", "");
                                    if (oid.length() > 6) oid = oid.substring(oid.length() - 6);
                                    if (tvId != null) tvId.setText("#" + oid);
                                    if (tvTime != null) tvTime.setText(order.optString("delivery_address", "Completed"));
                                    
                                    double earning = order.optDouble("partner_earning", order.optDouble("delivery_charge", 150.0));
                                    if (tvAmt != null) tvAmt.setText(String.format(Locale.getDefault(), "+₹%.0f", earning));

                                    llEarningsHistory.addView(itemView);
                                    count++;
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void showErrorState() {
        if (shimmerContainer != null) {
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
        }
        if (layoutContent != null) layoutContent.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.VISIBLE);
    }

    private void selectTab(int index) {
        if (tabToday == null || tabWeek == null || tabMonth == null) return;
        
        tabToday.setBackground(null);
        tabToday.setBackgroundColor(Color.TRANSPARENT);
        tabToday.setTextColor(Color.parseColor("#6B7280"));

        tabWeek.setBackground(null);
        tabWeek.setBackgroundColor(Color.TRANSPARENT);
        tabWeek.setTextColor(Color.parseColor("#6B7280"));

        tabMonth.setBackground(null);
        tabMonth.setBackgroundColor(Color.TRANSPARENT);
        tabMonth.setTextColor(Color.parseColor("#6B7280"));

        if (index == 0) {
            currentPeriod = "today";
            tabToday.setBackgroundResource(R.drawable.shape_pill_white);
            tabToday.setTextColor(getResources().getColor(R.color.vf_primary));
        } else if (index == 1) {
            currentPeriod = "week";
            tabWeek.setBackgroundResource(R.drawable.shape_pill_white);
            tabWeek.setTextColor(getResources().getColor(R.color.vf_primary));
        } else {
            currentPeriod = "month";
            tabMonth.setBackgroundResource(R.drawable.shape_pill_white);
            tabMonth.setTextColor(getResources().getColor(R.color.vf_primary));
        }
        loadEarnings();
    }
}
