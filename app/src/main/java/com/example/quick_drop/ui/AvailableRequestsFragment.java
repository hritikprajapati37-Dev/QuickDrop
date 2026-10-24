package com.example.quick_drop.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class AvailableRequestsFragment extends Fragment {

    private LinearLayout llRequests;
    private FusedLocationProviderClient fusedLocationClient;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isResumed()) {
                loadAvailableRequests();
                pollHandler.postDelayed(this, 3000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_available_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        llRequests = view.findViewById(R.id.llRequests);

        fetchAndUpdateGpsLocation();
        loadAvailableRequests();

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.availableRequestsFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.partnerDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_availableRequests_to_partnerDashboard);
                    return true;
                } else if (id == R.id.deliveryHistoryPartnerFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_availableRequests_to_history);
                    return true;
                } else if (id == R.id.earningsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_availableRequests_to_earnings);
                    return true;
                } else if (id == R.id.partnerProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_availableRequests_to_partnerProfile);
                    return true;
                }
                return false;
            });
        }
    }

    private void fetchAndUpdateGpsLocation() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        Log.d("QuickDrop_Diag", "AvailableRequestsFragment GPS: " + lat + ", " + lng);
                        ApiClient.updatePartnerLocation(token, lat, lng, null);
                    }
                });
            } catch (SecurityException ignored) {}
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAndUpdateGpsLocation();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.post(pollRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void loadAvailableRequests() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getAvailableRequests(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (llRequests != null) {
                        llRequests.removeAllViews();
                        JSONArray requests = response.optJSONArray("requests");
                        if (requests != null && requests.length() > 0) {
                            for (int i = 0; i < requests.length(); i++) {
                                JSONObject req = requests.optJSONObject(i);
                                if (req != null) {
                                    View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_delivery_request, llRequests, false);
                                    
                                    TextView tvOrderID = itemView.findViewById(R.id.tvOrderID);
                                    TextView tvEarnings = itemView.findViewById(R.id.tvEarnings);
                                    TextView tvDistance = itemView.findViewById(R.id.tvDistance);
                                    TextView tvPickupAddress = itemView.findViewById(R.id.tvPickupAddress);
                                    TextView tvDestAddress = itemView.findViewById(R.id.tvDestAddress);
                                    TextView tvPackageType = itemView.findViewById(R.id.tvPackageType);
                                    MaterialButton btnViewDetails = itemView.findViewById(R.id.btnViewDetails);

                                    String orderId = req.optString("order_id", "");
                                    String shortId = orderId;
                                    if (shortId.length() > 6) {
                                        shortId = shortId.substring(shortId.length() - 6);
                                    }
                                    if (tvOrderID != null) tvOrderID.setText("#" + shortId);

                                    double partnerEarning = req.optDouble("partner_earning", req.optDouble("delivery_charge", 0.0));
                                    if (tvEarnings != null) {
                                        tvEarnings.setText(String.format(Locale.getDefault(), "₹%.2f", partnerEarning));
                                    }

                                    double distKm = req.optDouble("distance_km", 0.0);
                                    if (tvDistance != null) {
                                        tvDistance.setText(String.format(Locale.getDefault(), "%.1f km away", distKm));
                                    }

                                    if (tvPickupAddress != null) tvPickupAddress.setText(req.optString("pickup_address", ""));
                                    if (tvDestAddress != null) tvDestAddress.setText(req.optString("delivery_address", ""));
                                    if (tvPackageType != null) tvPackageType.setText(req.optString("item_description", "Standard Package"));

                                    if (btnViewDetails != null) {
                                        btnViewDetails.setOnClickListener(v -> {
                                            Bundle args = new Bundle();
                                            args.putString("order_id", orderId);
                                            Navigation.findNavController(v).navigate(R.id.action_availableRequests_to_details, args);
                                        });
                                    }

                                    llRequests.addView(itemView);
                                }
                            }
                        } else {
                            TextView emptyTv = new TextView(requireContext());
                            emptyTv.setText("No delivery requests available right now.");
                            emptyTv.setPadding(32, 64, 32, 64);
                            emptyTv.setGravity(android.view.Gravity.CENTER);
                            llRequests.addView(emptyTv);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (llRequests != null) {
                        llRequests.removeAllViews();
                        TextView errorTv = new TextView(requireContext());
                        errorTv.setText("Failed to load available requests. Tap to retry.");
                        errorTv.setPadding(32, 64, 32, 64);
                        errorTv.setGravity(android.view.Gravity.CENTER);
                        errorTv.setOnClickListener(v -> loadAvailableRequests());
                        llRequests.addView(errorTv);
                    }
                });
            }
        });
    }
}
