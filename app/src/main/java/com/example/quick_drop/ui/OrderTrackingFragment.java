package com.example.quick_drop.ui;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OrderTrackingFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapViewTracking;
    private GoogleMap googleMap;
    private TextView tvHeaderOrderNum, tvStatusBadge, tvETA, tvDistance, tvPartnerName, tvPickupVal, tvDestVal;
    private ImageView ivStep1, ivStep2, ivStep3, ivStep4;
    private Marker partnerMarker;
    private LatLng currentPartnerLatLng;

    private double pickupLat = 12.9716, pickupLng = 77.5946;
    private double deliveryLat = 12.9352, deliveryLng = 77.6245;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isResumed()) {
                loadTrackingData();
                pollHandler.postDelayed(this, 3000);
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

        ivStep1 = view.findViewById(R.id.ivStep1);
        ivStep2 = view.findViewById(R.id.ivStep2);
        ivStep3 = view.findViewById(R.id.ivStep3);
        ivStep4 = view.findViewById(R.id.ivStep4);

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

        View btnCall = view.findViewById(R.id.btnCall);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> 
                Toast.makeText(getContext(), "Calling Delivery Partner...", Toast.LENGTH_SHORT).show());
        }

        View btnChat = view.findViewById(R.id.btnChat);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> 
                Toast.makeText(getContext(), "Chat feature coming soon!", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        updateMapMarkers();
    }

    private void updateMapMarkers() {
        if (googleMap == null) return;

        LatLng pickup = new LatLng(pickupLat, pickupLng);
        LatLng delivery = new LatLng(deliveryLat, deliveryLng);

        if (partnerMarker == null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(pickup).title("Pickup Location"));
            googleMap.addMarker(new MarkerOptions().position(delivery).title("Destination"));
            googleMap.addPolyline(new PolylineOptions().add(pickup, delivery).color(Color.parseColor("#4F46E5")).width(8f));

            LatLng center = new LatLng((pickupLat + deliveryLat) / 2.0, (pickupLng + deliveryLng) / 2.0);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13f));
        }

        if (currentPartnerLatLng != null) {
            if (partnerMarker == null) {
                partnerMarker = googleMap.addMarker(new MarkerOptions()
                        .position(currentPartnerLatLng)
                        .title("Delivery Partner")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            } else {
                animateMarkerTo(partnerMarker, currentPartnerLatLng);
            }
        }
    }

    private void animateMarkerTo(Marker marker, LatLng finalPosition) {
        if (marker == null || finalPosition == null) return;
        LatLng startPosition = marker.getPosition();
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(1200);
        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lat = v * finalPosition.latitude + (1 - v) * startPosition.latitude;
            double lng = v * finalPosition.longitude + (1 - v) * startPosition.longitude;
            marker.setPosition(new LatLng(lat, lng));
        });
        valueAnimator.start();
    }

    private void updateTimelineSteps(String status) {
        int checkedRes = R.drawable.ic_timeline_checked;
        int activeRes = R.drawable.ic_timeline_active;
        int pendingRes = R.drawable.ic_timeline_pending;

        if ("MATCHING".equalsIgnoreCase(status) || "CREATED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status) || "REFUNDED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            if (ivStep1 != null) ivStep1.setImageResource(pendingRes);
            if (ivStep2 != null) ivStep2.setImageResource(pendingRes);
            if (ivStep3 != null) ivStep3.setImageResource(pendingRes);
            if (ivStep4 != null) ivStep4.setImageResource(pendingRes);
        } else if ("ASSIGNED".equalsIgnoreCase(status)) {
            if (ivStep1 != null) ivStep1.setImageResource(checkedRes);
            if (ivStep2 != null) ivStep2.setImageResource(activeRes);
            if (ivStep3 != null) ivStep3.setImageResource(pendingRes);
            if (ivStep4 != null) ivStep4.setImageResource(pendingRes);
        } else if ("PICKED_UP".equalsIgnoreCase(status) || "IN_TRANSIT".equalsIgnoreCase(status)) {
            if (ivStep1 != null) ivStep1.setImageResource(checkedRes);
            if (ivStep2 != null) ivStep2.setImageResource(checkedRes);
            if (ivStep3 != null) ivStep3.setImageResource(activeRes);
            if (ivStep4 != null) ivStep4.setImageResource(pendingRes);
        } else if ("DELIVERED".equalsIgnoreCase(status)) {
            if (ivStep1 != null) ivStep1.setImageResource(checkedRes);
            if (ivStep2 != null) ivStep2.setImageResource(checkedRes);
            if (ivStep3 != null) ivStep3.setImageResource(checkedRes);
            if (ivStep4 != null) ivStep4.setImageResource(checkedRes);
        }
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
                            String deadlineIso = order.optString("acceptance_deadline_at", "");

                            updateTimelineSteps(status);

                            if ("MATCHING".equalsIgnoreCase(status) || "CREATED".equalsIgnoreCase(status)) {
                                long remainingSecs = getRemainingSeconds(deadlineIso);
                                long mins = remainingSecs / 60;
                                long secs = remainingSecs % 60;

                                if (tvStatusBadge != null) tvStatusBadge.setText("FINDING PARTNER");
                                if (tvETA != null) {
                                    if (remainingSecs > 0) {
                                        tvETA.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                                    } else {
                                        tvETA.setText("Expiring...");
                                    }
                                }
                                if (tvPartnerName != null) tvPartnerName.setText("Searching Nearest Partner...");
                            } else if ("EXPIRED".equalsIgnoreCase(status) || "REFUNDED".equalsIgnoreCase(status) || "DRAFT_EXPIRED".equalsIgnoreCase(status)) {
                                double amt = order.optDouble("total_amount", 0.0);
                                if (tvStatusBadge != null) tvStatusBadge.setText("MATCHING EXPIRED");
                                if (tvETA != null) tvETA.setText("Demo Refund");
                                if (tvPartnerName != null) tvPartnerName.setText(String.format(Locale.getDefault(), "Demo Refund Recorded (₹%.2f)", amt));
                                currentPartnerLatLng = null;
                            } else {
                                if (tvStatusBadge != null) tvStatusBadge.setText(status);
                                if (tvETA != null) tvETA.setText("On Time");

                                String partner = order.optString("assigned_partner", "");
                                if (tvPartnerName != null) {
                                    tvPartnerName.setText(partner.isEmpty() || "null".equals(partner) ? "Partner Assigned" : "Partner: " + partner);
                                }

                                double pLat = order.optDouble("partner_lat", 0.0);
                                double pLng = order.optDouble("partner_lng", 0.0);
                                if (pLat != 0.0 && pLng != 0.0) {
                                    currentPartnerLatLng = new LatLng(pLat, pLng);
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
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private long getRemainingSeconds(String deadlineIso) {
        if (deadlineIso == null || deadlineIso.isEmpty()) return 300L;
        try {
            String cleanIso = deadlineIso.replace("Z", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date;
            try {
                date = sdf.parse(cleanIso);
            } catch (Exception e) {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                date = sdf2.parse(cleanIso);
            }
            if (date != null) {
                long remainingMillis = date.getTime() - System.currentTimeMillis();
                return Math.max(0, remainingMillis / 1000L);
            }
        } catch (Exception ignored) {}
        return 300L;
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
