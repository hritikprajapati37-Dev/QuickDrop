package com.example.quick_drop.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class OrderTrackingFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapViewTracking;
    private GoogleMap googleMap;
    private TextView tvHeaderOrderNum, tvStatusBadge, tvETA, tvDistance, tvPartnerName, tvPickupVal, tvDestVal;
    private double pickupLat = 12.9716, pickupLng = 77.5946;
    private double deliveryLat = 12.9352, deliveryLng = 77.6245;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isResumed()) {
                loadTrackingData();
                pollHandler.postDelayed(this, 4000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_tracking, container, false);

        mapViewTracking = view.findViewById(R.id.mapViewTracking);
        if (mapViewTracking != null) {
            mapViewTracking.onCreate(savedInstanceState);
            mapViewTracking.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        tvHeaderOrderNum = view.findViewById(R.id.tvHeaderOrderNum);
        tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
        tvETA = view.findViewById(R.id.tvETA);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvPartnerName = view.findViewById(R.id.tvPartnerName);
        tvPickupVal = view.findViewById(R.id.tvPickupVal);
        tvDestVal = view.findViewById(R.id.tvDestVal);

        loadTrackingData();

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.deliveriesFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.senderDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_tracking_to_senderDashboard);
                    return true;
                } else if (id == R.id.deliveriesFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_tracking_to_deliveries);
                    return true;
                } else if (id == R.id.orderHistorySenderFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_tracking_to_orderHistory);
                    return true;
                } else if (id == R.id.senderProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_tracking_to_senderProfile);
                    return true;
                }
                return false;
            });
        }

        view.findViewById(R.id.btnCall).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Calling Delivery Partner...", Toast.LENGTH_SHORT).show());
            
        view.findViewById(R.id.btnChat).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Chat feature coming soon!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        updateMapMarkers();
    }

    private void updateMapMarkers() {
        if (googleMap == null) return;
        googleMap.clear();

        LatLng pickup = new LatLng(pickupLat, pickupLng);
        LatLng delivery = new LatLng(deliveryLat, deliveryLng);

        googleMap.addMarker(new MarkerOptions().position(pickup).title("Pickup Location"));
        googleMap.addMarker(new MarkerOptions().position(delivery).title("Destination"));
        googleMap.addPolyline(new PolylineOptions().add(pickup, delivery).color(Color.parseColor("#4F46E5")).width(8f));

        LatLng center = new LatLng((pickupLat + deliveryLat) / 2.0, (pickupLng + deliveryLng) / 2.0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13f));
    }

    private void loadTrackingData() {
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
                            String orderId = order.optString("order_id", "");
                            String shortId = orderId;
                            if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                            if (tvHeaderOrderNum != null) tvHeaderOrderNum.setText("Order #" + shortId);

                            String status = order.optString("status", "MATCHING");
                            if (tvStatusBadge != null) {
                                if ("EXPIRED".equalsIgnoreCase(status)) {
                                    tvStatusBadge.setText("MATCHING EXPIRED (REFUNDED)");
                                } else {
                                    tvStatusBadge.setText(status);
                                }
                            }

                            pickupLat = order.optDouble("pickup_lat", 12.9716);
                            pickupLng = order.optDouble("pickup_lng", 77.5946);
                            deliveryLat = order.optDouble("delivery_lat", 12.9352);
                            deliveryLng = order.optDouble("delivery_lng", 77.6245);

                            updateMapMarkers();

                            if (tvPickupVal != null) tvPickupVal.setText(order.optString("pickup_address", ""));
                            if (tvDestVal != null) tvDestVal.setText(order.optString("delivery_address", ""));

                            double dist = order.optDouble("distance_km", 0.0);
                            if (tvDistance != null) {
                                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", dist));
                            }
                            if (tvETA != null) {
                                tvETA.setText("EXPIRED".equalsIgnoreCase(status) ? "Refund Issued" : "On Time");
                            }

                            String partner = order.optString("assigned_partner", "");
                            if (tvPartnerName != null) {
                                if (partner.isEmpty() || "null".equals(partner)) {
                                    if ("EXPIRED".equalsIgnoreCase(status)) {
                                        tvPartnerName.setText("No Partner Available (Refunded)");
                                    } else {
                                        tvPartnerName.setText("Searching Nearest Partner...");
                                    }
                                } else {
                                    tvPartnerName.setText(partner);
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

    @Override
    public void onResume() {
        super.onResume();
        if (mapViewTracking != null) mapViewTracking.onResume();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.post(pollRunnable);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapViewTracking != null) mapViewTracking.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapViewTracking != null) mapViewTracking.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapViewTracking != null) mapViewTracking.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapViewTracking != null) mapViewTracking.onDestroy();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewTracking != null) mapViewTracking.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapViewTracking != null) mapViewTracking.onSaveInstanceState(outState);
    }
}
