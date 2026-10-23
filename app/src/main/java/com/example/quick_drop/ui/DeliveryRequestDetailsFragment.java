package com.example.quick_drop.ui;

import android.graphics.Color;
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
import org.json.JSONObject;
import java.util.Locale;

public class DeliveryRequestDetailsFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapViewDetails;
    private GoogleMap googleMap;
    private TextView tvEarnings, tvOrderID, tvEstTime, tvDistanceAway, tvPickupAddress, tvDestAddress, tvItemContents, tvItemWeight;
    private MaterialButton btnAccept;
    private String orderId = "";
    private double pickupLat = 12.9716, pickupLng = 77.5946;
    private double deliveryLat = 12.9352, deliveryLng = 77.6245;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_request_details, container, false);

        mapViewDetails = view.findViewById(R.id.mapViewDetails);
        if (mapViewDetails != null) {
            mapViewDetails.onCreate(savedInstanceState);
            mapViewDetails.getMapAsync(this);
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

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvEarnings = view.findViewById(R.id.tvEarnings);
        tvOrderID = view.findViewById(R.id.tvOrderID);
        tvEstTime = view.findViewById(R.id.tvEstTime);
        tvDistanceAway = view.findViewById(R.id.tvDistanceAway);
        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvDestAddress = view.findViewById(R.id.tvDestAddress);
        tvItemContents = view.findViewById(R.id.tvItemContents);
        tvItemWeight = view.findViewById(R.id.tvItemWeight);
        btnAccept = view.findViewById(R.id.btnAccept);

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
        }

        loadRequestDetails();

        if (btnAccept != null) {
            btnAccept.setOnClickListener(this::acceptDelivery);
        }
    }

    private void loadRequestDetails() {
        if (orderId.isEmpty()) return;
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getOrderDetails(token, orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject order) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> bindOrderData(order));
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Failed to load details: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void bindOrderData(JSONObject order) {
        String oid = order.optString("order_id", orderId);
        String shortId = oid;
        if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
        if (tvOrderID != null) tvOrderID.setText("#" + shortId);

        double earnings = order.optDouble("partner_earning", order.optDouble("delivery_charge", 0.0));
        if (tvEarnings != null) {
            tvEarnings.setText(String.format(Locale.getDefault(), "₹%.2f", earnings));
        }

        double dist = order.optDouble("distance_km", 0.0);
        if (tvDistanceAway != null) {
            tvDistanceAway.setText(String.format(Locale.getDefault(), "%.1f km away", dist));
        }

        if (tvPickupAddress != null) tvPickupAddress.setText(order.optString("pickup_address", ""));
        if (tvDestAddress != null) tvDestAddress.setText(order.optString("delivery_address", ""));
        if (tvItemContents != null) tvItemContents.setText(order.optString("item_description", "Standard Package"));
        if (tvItemWeight != null) tvItemWeight.setText(order.optString("item_weight", "1.0 kg"));

        pickupLat = order.optDouble("pickup_lat", 12.9716);
        pickupLng = order.optDouble("pickup_lng", 77.5946);
        deliveryLat = order.optDouble("delivery_lat", 12.9352);
        deliveryLng = order.optDouble("delivery_lng", 77.6245);

        updateMap();
    }

    private void acceptDelivery(View v) {
        if (orderId.isEmpty()) return;
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (btnAccept != null) btnAccept.setEnabled(false);

        ApiClient.acceptRequest(token, orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Delivery accepted successfully!", Toast.LENGTH_SHORT).show();
                    try {
                        Navigation.findNavController(v).navigate(R.id.action_details_to_destDelivery);
                    } catch (Exception e) {
                        Navigation.findNavController(v).navigateUp();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (btnAccept != null) btnAccept.setEnabled(true);
                    Toast.makeText(getContext(), "Acceptance failed: " + error, Toast.LENGTH_LONG).show();
                    Navigation.findNavController(v).navigateUp();
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
        LatLng delivery = new LatLng(deliveryLat, deliveryLng);

        googleMap.addMarker(new MarkerOptions().position(pickup).title("Pickup Location"));
        googleMap.addMarker(new MarkerOptions().position(delivery).title("Destination"));
        googleMap.addPolyline(new PolylineOptions().add(pickup, delivery).color(Color.parseColor("#4F46E5")).width(8f));

        LatLng center = new LatLng((pickupLat + deliveryLat) / 2.0, (pickupLng + deliveryLng) / 2.0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13f));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapViewDetails != null) mapViewDetails.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapViewDetails != null) mapViewDetails.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapViewDetails != null) mapViewDetails.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapViewDetails != null) mapViewDetails.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapViewDetails != null) mapViewDetails.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewDetails != null) mapViewDetails.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapViewDetails != null) mapViewDetails.onSaveInstanceState(outState);
    }
}
