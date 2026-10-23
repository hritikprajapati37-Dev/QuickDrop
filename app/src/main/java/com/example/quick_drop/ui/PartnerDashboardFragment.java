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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.json.JSONArray;
import org.json.JSONObject;

public class PartnerDashboardFragment extends Fragment {

    private View cardActiveDelivery;
    private TextView tvAvailableCount, tvCompletedDeliveries, tvRejectedCount, tvAvailableSummary, tvOnlineStatus;
    private TextView tvOrderID, tvPickupVal, tvDestVal, tvETA;
    private MaterialSwitch switchOnline;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvGreeting = view.findViewById(R.id.tvGreetingPartner);
        if (tvGreeting != null) {
            String name = UserSession.getUserName(requireContext());
            if (name != null && !name.isEmpty()) {
                tvGreeting.setText("Good Morning, " + name.split(" ")[0]);
            } else {
                tvGreeting.setText("Good Morning, Partner");
            }
        }

        tvAvailableCount = view.findViewById(R.id.tvAvailableCount);
        tvCompletedDeliveries = view.findViewById(R.id.tvCompletedDeliveries);
        tvRejectedCount = view.findViewById(R.id.tvRejectedCount);
        tvAvailableSummary = view.findViewById(R.id.tvAvailableSummary);
        cardActiveDelivery = view.findViewById(R.id.cardActiveOrderPartner);
        tvOrderID = view.findViewById(R.id.tvOrderIDPartner);
        tvPickupVal = view.findViewById(R.id.tvPickupPartner);
        tvDestVal = view.findViewById(R.id.tvDestPartner);
        tvETA = view.findViewById(R.id.tvETAPartner);
        tvOnlineStatus = view.findViewById(R.id.tvOnlineStatus);
        switchOnline = view.findViewById(R.id.switchOnline);

        if (cardActiveDelivery != null) {
            cardActiveDelivery.setVisibility(View.GONE);
        }

        if (switchOnline != null) {
            switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (tvOnlineStatus != null) {
                    tvOnlineStatus.setText(isChecked ? "You're Online" : "You're Offline");
                }
                String token = UserSession.getToken(requireContext());
                if (!token.isEmpty()) {
                    ApiClient.updatePartnerStatus(token, isChecked, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), isChecked ? "You are now ONLINE" : "You are now OFFLINE", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {}
                    });
                }
            });
        }

        loadPartnerDashboard();

        view.findViewById(R.id.btnViewDelivery).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_availableRequests));

        view.findViewById(R.id.btnViewRequests).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_availableRequests));

        view.findViewById(R.id.ivPartnerProfile).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_profile));

        View cardAddress = view.findViewById(R.id.cardAddressPartner);
        if (cardAddress != null) {
            cardAddress.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_partnerMap));
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.partnerDashboardFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.availableRequestsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_availableRequests);
                    return true;
                } else if (id == R.id.deliveryHistoryPartnerFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_history);
                    return true;
                } else if (id == R.id.earningsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_earnings);
                    return true;
                } else if (id == R.id.partnerProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_profile);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadPartnerDashboard() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getProfile(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    boolean isOnline = response.optBoolean("is_online", true);
                    if (switchOnline != null) {
                        switchOnline.setChecked(isOnline);
                    }
                    if (tvOnlineStatus != null) {
                        tvOnlineStatus.setText(isOnline ? "You're Online" : "You're Offline");
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });

        ApiClient.getDashboard(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONObject counts = response.optJSONObject("counts");
                    if (counts != null) {
                        if (tvCompletedDeliveries != null) tvCompletedDeliveries.setText(String.valueOf(counts.optInt("completed", 0)));
                        if (tvRejectedCount != null) tvRejectedCount.setText(String.valueOf(counts.optInt("cancelled", counts.optInt("rejected", 0))));
                    }

                    JSONObject activeDelivery = response.optJSONObject("active_delivery");
                    if (activeDelivery != null) {
                        if (cardActiveDelivery != null) cardActiveDelivery.setVisibility(View.VISIBLE);
                        if (tvOrderID != null) {
                            String oid = activeDelivery.optString("order_id", "");
                            if (oid.length() > 6) oid = oid.substring(oid.length() - 6);
                            tvOrderID.setText("#" + oid);
                        }
                        if (tvPickupVal != null) tvPickupVal.setText(activeDelivery.optString("pickup_address", ""));
                        if (tvDestVal != null) tvDestVal.setText(activeDelivery.optString("delivery_address", ""));
                        if (tvETA != null) tvETA.setText("In Progress");
                    } else {
                        if (cardActiveDelivery != null) cardActiveDelivery.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (cardActiveDelivery != null) cardActiveDelivery.setVisibility(View.GONE);
                });
            }
        });

        ApiClient.getAvailableRequests(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONArray reqs = response.optJSONArray("requests");
                    int count = reqs != null ? reqs.length() : 0;
                    if (tvAvailableCount != null) {
                        tvAvailableCount.setText(String.valueOf(count));
                    }
                    if (tvAvailableSummary != null) {
                        tvAvailableSummary.setText(count + " delivery request" + (count == 1 ? "" : "s") + " available");
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }
}
