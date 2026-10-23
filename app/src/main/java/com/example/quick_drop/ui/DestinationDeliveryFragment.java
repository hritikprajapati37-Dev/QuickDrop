package com.example.quick_drop.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class DestinationDeliveryFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapViewDest;
    private GoogleMap googleMap;
    private TextView tvOrderID, tvPickupAddress, tvDistanceAway;
    private MaterialButton btnNavigate, btnArrived;

    private String orderId = "";
    private double pickupLat = 12.9716, pickupLng = 77.5946;
    private double deliveryLat = 12.9352, deliveryLng = 77.6245;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_destination_delivery, container, false);

        mapViewDest = view.findViewById(R.id.mapViewDest);
        if (mapViewDest != null) {
            mapViewDest.onCreate(savedInstanceState);
            mapViewDest.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvOrderID = view.findViewById(R.id.tvOrderID);
        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvDistanceAway = view.findViewById(R.id.tvDistanceAway);
        btnNavigate = view.findViewById(R.id.btnNavigate);
        btnArrived = view.findViewById(R.id.btnArrived);

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
        }

        loadOrderDetails();

        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> launchGoogleMapsNavigation());
        }

        if (btnArrived != null) {
            btnArrived.setOnClickListener(this::arrivedAtPickup);
        }
    }

    private void loadOrderDetails() {
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
                            if (orderId.isEmpty()) {
                                orderId = order.optString("order_id", "");
                            }
                            String shortId = orderId;
                            if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                            if (tvOrderID != null) tvOrderID.setText("#" + shortId);

                            pickupLat = order.optDouble("pickup_lat", 12.9716);
                            pickupLng = order.optDouble("pickup_lng", 77.5946);
                            deliveryLat = order.optDouble("delivery_lat", 12.9352);
                            deliveryLng = order.optDouble("delivery_lng", 77.6245);

                            if (tvPickupAddress != null) {
                                tvPickupAddress.setText(order.optString("pickup_address", ""));
                            }

                            double dist = order.optDouble("distance_km", 0.0);
                            if (tvDistanceAway != null) {
                                tvDistanceAway.setText(String.format(Locale.getDefault(), "%.1f km away", dist));
                            }

                            updateMap();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void launchGoogleMapsNavigation() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + pickupLat + "," + pickupLng + "&mode=d");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + pickupLat + "," + pickupLng);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void arrivedAtPickup(View v) {
        if (orderId.isEmpty()) return;
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (btnArrived != null) btnArrived.setEnabled(false);

        ApiClient.arrivedPickup(token, orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Arrived at pickup! Please enter OTP.", Toast.LENGTH_SHORT).show();
                    try {
                        Bundle args = new Bundle();
                        args.putString("order_id", orderId);
                        Navigation.findNavController(v).navigate(R.id.action_destDelivery_to_pickupConfirmation, args);
                    } catch (Exception e) {
                        Navigation.findNavController(v).navigateUp();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (btnArrived != null) btnArrived.setEnabled(true);
                    Toast.makeText(getContext(), "Failed to record arrival: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        updateMap();
    }

    private void updateMap() {
        if (googleMap == null) return;
        googleMap.clear();

        LatLng pickup = new LatLng(pickupLat, pickupLng);
        googleMap.addMarker(new MarkerOptions().position(pickup).title("Pickup Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 14f));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapViewDest != null) mapViewDest.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapViewDest != null) mapViewDest.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapViewDest != null) mapViewDest.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapViewDest != null) mapViewDest.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapViewDest != null) mapViewDest.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewDest != null) mapViewDest.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapViewDest != null) mapViewDest.onSaveInstanceState(outState);
    }
}
