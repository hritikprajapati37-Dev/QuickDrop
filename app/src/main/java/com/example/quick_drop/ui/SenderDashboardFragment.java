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
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONObject;

public class SenderDashboardFragment extends Fragment {

    private ShimmerFrameLayout shimmerContainer;
    private View layoutContent, layoutError;
    private TextView tvGreeting, tvOrderID, tvPickupVal, tvDestVal;
    private TextView tvActiveCount, tvCompletedCount, tvPendingCount;
    private View cardActiveOrder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sender_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        layoutContent = view.findViewById(R.id.layoutContent);
        layoutError = view.findViewById(R.id.layoutError);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvOrderID = view.findViewById(R.id.tvOrderID);
        tvPickupVal = view.findViewById(R.id.tvPickupVal);
        tvDestVal = view.findViewById(R.id.tvDestVal);
        tvActiveCount = view.findViewById(R.id.tvActiveCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        cardActiveOrder = view.findViewById(R.id.cardActiveOrder);

        // Set initial greeting from UserSession before API loads
        String localName = UserSession.getUserName(requireContext());
        if (tvGreeting != null) {
            String firstName = (localName != null && !localName.isEmpty()) ? localName.split(" ")[0] : "User";
            tvGreeting.setText("Good Morning, " + firstName);
        }

        View btnRetry = view.findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadDashboard());
        }

        loadDashboard();

        view.findViewById(R.id.btnCreateDelivery).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_createDelivery));

        view.findViewById(R.id.ivProfile).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_profile));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
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
        }

        view.findViewById(R.id.btnViewActiveDetails).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_tracking));
            
        view.findViewById(R.id.cardActiveOrder).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_orderDetails));

        view.findViewById(R.id.btnViewAllOrders).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderDashboard_to_orderHistory));
    }

    private void loadDashboard() {
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

        ApiClient.getDashboard(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);

                    String name = response.optString("name", "");
                    if (name.isEmpty()) name = UserSession.getUserName(requireContext());
                    if (name.isEmpty()) name = "User";
                    String firstName = name.split(" ")[0];
                    if (tvGreeting != null) {
                        tvGreeting.setText("Good Morning, " + firstName);
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

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showErrorState());
            }
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
}
