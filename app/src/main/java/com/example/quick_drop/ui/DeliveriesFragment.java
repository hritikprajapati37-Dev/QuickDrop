package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import org.json.JSONObject;

public class DeliveriesFragment extends Fragment {

    private View cardDelivery;
    private TextView tvOrderID, tvPickupVal, tvDestVal, tvETA, tvActiveCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_deliveries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        cardDelivery = view.findViewById(R.id.cardDelivery);
        tvOrderID = view.findViewById(R.id.tvOrderID);
        tvPickupVal = view.findViewById(R.id.tvPickupVal);
        tvDestVal = view.findViewById(R.id.tvDestVal);
        tvETA = view.findViewById(R.id.tvETA);
        tvActiveCount = view.findViewById(R.id.tvActiveCount);

        if (cardDelivery != null) {
            cardDelivery.setVisibility(View.GONE);
        }

        loadDeliveries();

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.deliveriesFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.senderDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_deliveries_to_senderDashboard);
                    return true;
                } else if (id == R.id.orderHistorySenderFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_deliveries_to_orderHistory);
                    return true;
                } else if (id == R.id.senderProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_deliveries_to_senderProfile);
                    return true;
                }
                return false;
            });
        }

        View btnTrack = view.findViewById(R.id.btnTrack);
        if (btnTrack != null) {
            btnTrack.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_deliveries_to_tracking));
        }
        
        View btnDetails = view.findViewById(R.id.btnDetails);
        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_deliveries_to_orderDetails));
        }
    }

    private void loadDeliveries() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getDashboard(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONObject counts = response.optJSONObject("counts");
                    int activeCount = counts != null ? counts.optInt("active", 0) : 0;
                    if (tvActiveCount != null) {
                        tvActiveCount.setText(activeCount + " active delivery" + (activeCount == 1 ? "" : "s"));
                    }

                    JSONObject activeDelivery = response.optJSONObject("active_delivery");
                    if (activeDelivery != null) {
                        if (cardDelivery != null) cardDelivery.setVisibility(View.VISIBLE);
                        if (tvOrderID != null) {
                            String oid = activeDelivery.optString("order_id", "");
                            if (oid.length() > 6) {
                                oid = oid.substring(oid.length() - 6);
                            }
                            tvOrderID.setText("#" + oid);
                        }
                        if (tvPickupVal != null) {
                            tvPickupVal.setText(activeDelivery.optString("pickup_address", ""));
                        }
                        if (tvDestVal != null) {
                            tvDestVal.setText(activeDelivery.optString("delivery_address", ""));
                        }
                        if (tvETA != null) {
                            tvETA.setText("In Progress");
                        }
                    } else {
                        if (cardDelivery != null) cardDelivery.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (cardDelivery != null) cardDelivery.setVisibility(View.GONE);
                    if (tvActiveCount != null) {
                        tvActiveCount.setText("0 active deliveries");
                    }
                });
            }
        });
    }
}
