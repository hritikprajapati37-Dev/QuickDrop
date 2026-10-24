package com.example.quick_drop.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

public class CreateDeliveryFragment extends Fragment {

    private TextInputEditText etReceiverPhone, etDeliveryAddress, etItemWeight;
    private AutoCompleteTextView actvCategory;
    private TextView tvPickupPrimary, tvPickupSecondary;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 12.9716;
    private double currentLng = 77.5946;
    private String pickupAddressStr = "";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_delivery, container, false);
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

        String[] categories = new String[] {"Documents", "Electronics", "Food", "Fragile", "Clothing", "General"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory = view.findViewById(R.id.actvCategory);
        if (actvCategory != null) {
            actvCategory.setAdapter(adapter);
        }

        etReceiverPhone = view.findViewById(R.id.etReceiverPhone);
        etDeliveryAddress = view.findViewById(R.id.etDeliveryAddress);
        etItemWeight = view.findViewById(R.id.etItemWeight);
        tvPickupPrimary = view.findViewById(R.id.tvPickupPrimary);
        tvPickupSecondary = view.findViewById(R.id.tvPickupSecondary);

        TextInputLayout tilDeliveryAddress = view.findViewById(R.id.tilDeliveryAddress);
        if (tilDeliveryAddress != null) {
            tilDeliveryAddress.setEndIconOnClickListener(v -> showLocationPickerOptions());
        }

        // Check if user has a last-used pickup address saved in UserSession
        String lastAddress = UserSession.getLastPickupAddress(requireContext());
        if (lastAddress != null && !lastAddress.isEmpty()) {
            pickupAddressStr = lastAddress;
            if (tvPickupPrimary != null) tvPickupPrimary.setText(pickupAddressStr);
            if (tvPickupSecondary != null) tvPickupSecondary.setText("Default (Last used address)");
        } else {
            if (tvPickupPrimary != null) tvPickupPrimary.setText("No pickup location selected");
            if (tvPickupSecondary != null) tvPickupSecondary.setText("Tap 'Use Current Location' or 'Enter Manually'");
            loadPastPickupAddress();
        }

        MaterialButton btnUseCurrentLocation = view.findViewById(R.id.btnUseCurrentLocation);
        if (btnUseCurrentLocation != null) {
            btnUseCurrentLocation.setOnClickListener(v -> checkGpsAndFetchLocation());
        }

        MaterialButton btnEnterManualPickup = view.findViewById(R.id.btnEnterManualPickup);
        if (btnEnterManualPickup != null) {
            btnEnterManualPickup.setOnClickListener(v -> showManualPickupDialog());
        }

        View btnNext = view.findViewById(R.id.btnNext);
        if (btnNext != null) {
            btnNext.setOnClickListener(this::submitOrder);
        }
    }

    private void showManualPickupDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manual_address, null);
        EditText etManualAddress = dialogView.findViewById(R.id.etManualAddress);
        if (etManualAddress != null && !pickupAddressStr.isEmpty() && !pickupAddressStr.contains("No pickup")) {
            etManualAddress.setText(pickupAddressStr);
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Pickup Address Manually")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                if (etManualAddress != null) {
                    String manualStr = etManualAddress.getText().toString().trim();
                    if (!manualStr.isEmpty()) {
                        pickupAddressStr = manualStr;
                        UserSession.setLastPickupAddress(requireContext(), pickupAddressStr);
                        if (tvPickupPrimary != null) tvPickupPrimary.setText(pickupAddressStr);
                        if (tvPickupSecondary != null) tvPickupSecondary.setText("Manually entered address");
                        Toast.makeText(getContext(), "Pickup address updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Address cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showLocationPickerOptions() {
        CharSequence[] options = new CharSequence[]{"Type Delivery Address Manually", "Pick Location on Map"};
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Delivery Location Method")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    if (etDeliveryAddress != null) {
                        etDeliveryAddress.requestFocus();
                        Toast.makeText(getContext(), "Please type your complete delivery address", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showMapPickerDialog();
                }
            })
            .show();
    }

    private void showMapPickerDialog() {
        double pickupLat = currentLat;
        double pickupLng = currentLng;
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(pickupAddressStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                pickupLat = addr.getLatitude();
                pickupLng = addr.getLongitude();
            }
        } catch (Exception e) {
            // fallback
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_map_picker, null);
        MapView dialogMapView = dialogView.findViewById(R.id.dialogMapView);
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Destination (Centered on Pickup - 5 km limit)")
            .setView(dialogView)
            .setNegativeButton("Cancel", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        final double finalPickupLat = pickupLat;
        final double finalPickupLng = pickupLng;

        if (dialogMapView != null) {
            dialogMapView.onCreate(null);
            dialogMapView.onResume();
            dialogMapView.getMapAsync(map -> {
                LatLng pickupLoc = new LatLng(finalPickupLat, finalPickupLng);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLoc, 14f));
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setCompassEnabled(true);

                try {
                    map.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    // ignore
                }

                map.addMarker(new MarkerOptions()
                    .position(pickupLoc)
                    .title("Pickup Location")
                    .snippet("Delivery allowed within 5 km radius"));

                map.addCircle(new CircleOptions()
                    .center(pickupLoc)
                    .radius(5000.0)
                    .strokeColor(Color.argb(255, 33, 150, 243))
                    .fillColor(Color.argb(35, 33, 150, 243))
                    .strokeWidth(3f));

                map.setOnMapClickListener(latLng -> {
                    map.clear();
                    map.addMarker(new MarkerOptions().position(pickupLoc).title("Pickup Location"));
                    map.addCircle(new CircleOptions()
                        .center(pickupLoc)
                        .radius(5000.0)
                        .strokeColor(Color.argb(255, 33, 150, 243))
                        .fillColor(Color.argb(35, 33, 150, 243))
                        .strokeWidth(3f));

                    map.addMarker(new MarkerOptions().position(latLng).title("Selected Delivery Location"));
                    
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> addrs = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        String addrStr = "Location (" + String.format(Locale.getDefault(), "%.4f", latLng.latitude) + ", " + String.format(Locale.getDefault(), "%.4f", latLng.longitude) + ")";
                        if (addrs != null && !addrs.isEmpty()) {
                            String line = addrs.get(0).getAddressLine(0);
                            if (line != null) addrStr = line;
                        }
                        
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(finalPickupLat, finalPickupLng, latLng.latitude, latLng.longitude, results);
                        float distKm = results[0] / 1000f;
                        
                        if (distKm > 5.0f) {
                            Toast.makeText(getContext(), "Selected location is out of range (> 5 km from pickup: " + String.format(Locale.getDefault(), "%.1f", distKm) + " km)", Toast.LENGTH_LONG).show();
                        } else {
                            if (etDeliveryAddress != null) etDeliveryAddress.setText(addrStr);
                            Toast.makeText(getContext(), "Delivery location selected successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    } catch (Exception e) {
                        if (etDeliveryAddress != null) etDeliveryAddress.setText("Lat: " + latLng.latitude + ", Lng: " + latLng.longitude);
                        dialog.dismiss();
                    }
                });
            });
        }
    }

    private void loadPastPickupAddress() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONArray orders = response.optJSONArray("orders");
                    if (orders != null && orders.length() > 0) {
                        JSONObject latestOrder = orders.optJSONObject(0);
                        if (latestOrder != null) {
                            String pastPickup = latestOrder.optString("pickup_address", "");
                            if (!pastPickup.isEmpty() && !pastPickup.contains("No pickup")) {
                                if (pickupAddressStr.isEmpty() || pickupAddressStr.contains("No pickup")) {
                                    pickupAddressStr = pastPickup;
                                    UserSession.setLastPickupAddress(requireContext(), pickupAddressStr);
                                    if (tvPickupPrimary != null) tvPickupPrimary.setText(pickupAddressStr);
                                    if (tvPickupSecondary != null) tvPickupSecondary.setText("From your previous order");
                                }
                                currentLat = latestOrder.optDouble("pickup_lat", 12.9716);
                                currentLng = latestOrder.optDouble("pickup_lng", 77.5946);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void checkGpsAndFetchLocation() {
        if (!isGpsEnabled()) {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("GPS is Disabled")
                .setMessage("Please enable GPS/Location services to use your current location for pickup.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    reverseGeocode(currentLat, currentLng);
                } else {
                    Toast.makeText(getContext(), "Unable to retrieve current location. Using GPS.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void reverseGeocode(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String line1 = address.getAddressLine(0);
                if (line1 != null && !line1.isEmpty()) {
                    pickupAddressStr = line1;
                } else {
                    pickupAddressStr = "Lat: " + lat + ", Lng: " + lng;
                }
            } else {
                pickupAddressStr = "GPS Location (" + String.format(Locale.getDefault(), "%.4f", lat) + ", " + String.format(Locale.getDefault(), "%.4f", lng) + ")";
            }

            UserSession.setLastPickupAddress(requireContext(), pickupAddressStr);

            if (tvPickupPrimary != null) {
                tvPickupPrimary.setText(pickupAddressStr);
            }
            if (tvPickupSecondary != null) {
                tvPickupSecondary.setText("Lat: " + String.format(Locale.getDefault(), "%.4f", lat) + ", Lng: " + String.format(Locale.getDefault(), "%.4f", lng));
            }
            Toast.makeText(getContext(), "Current location set as pickup!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            pickupAddressStr = "GPS Location (" + lat + ", " + lng + ")";
            UserSession.setLastPickupAddress(requireContext(), pickupAddressStr);
            if (tvPickupPrimary != null) tvPickupPrimary.setText(pickupAddressStr);
            if (tvPickupSecondary != null) tvPickupSecondary.setText("Lat: " + lat + ", Lng: " + lng);
            Toast.makeText(getContext(), "Current location captured successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGpsAndFetchLocation();
            } else {
                Toast.makeText(getContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitOrder(View v) {
        String receiverPhone = etReceiverPhone != null && etReceiverPhone.getText() != null ? etReceiverPhone.getText().toString().trim() : "";
        String deliveryAddress = etDeliveryAddress != null && etDeliveryAddress.getText() != null ? etDeliveryAddress.getText().toString().trim() : "";
        String itemWeight = etItemWeight != null && etItemWeight.getText() != null ? etItemWeight.getText().toString().trim() : "1.0 kg";
        String category = actvCategory != null ? actvCategory.getText().toString().trim() : "General";

        boolean hasError = false;

        if (pickupAddressStr.isEmpty() || pickupAddressStr.contains("No pickup location")) {
            Toast.makeText(getContext(), "Please select a pickup location or use current location", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if (receiverPhone.isEmpty()) {
            if (etReceiverPhone != null) etReceiverPhone.setError("Receiver phone is required");
            hasError = true;
        } else if (receiverPhone.length() < 10) {
            if (etReceiverPhone != null) etReceiverPhone.setError("Enter a valid 10-digit phone number");
            hasError = true;
        }

        if (deliveryAddress.isEmpty()) {
            if (etDeliveryAddress != null) etDeliveryAddress.setError("Delivery address is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // Save last used pickup address
        UserSession.setLastPickupAddress(requireContext(), pickupAddressStr);

        // 1. Geocode pickupAddressStr to resolve true pickup coordinates
        double pLat = currentLat;
        double pLng = currentLng;
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> pickupAddrs = geocoder.getFromLocationName(pickupAddressStr, 1);
            if (pickupAddrs != null && !pickupAddrs.isEmpty()) {
                Address pAddr = pickupAddrs.get(0);
                pLat = pAddr.getLatitude();
                pLng = pAddr.getLongitude();
            }
        } catch (Exception e) {
            // fallback
        }

        // 2. Geocode deliveryAddress to resolve true delivery coordinates
        double deliveryLat = pLat + 0.01;
        double deliveryLng = pLng + 0.01;
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> deliveryAddrs = geocoder.getFromLocationName(deliveryAddress, 1);
            if (deliveryAddrs != null && !deliveryAddrs.isEmpty()) {
                Address dAddr = deliveryAddrs.get(0);
                deliveryLat = dAddr.getLatitude();
                deliveryLng = dAddr.getLongitude();
            }
        } catch (Exception e) {
            // fallback offset
        }

        // 3. Compute accurate distance between true pickup and true delivery locations
        float[] results = new float[1];
        android.location.Location.distanceBetween(pLat, pLng, deliveryLat, deliveryLng, results);
        float distanceInKm = results[0] / 1000f;

        if (distanceInKm > 5.0f) {
            if (etDeliveryAddress != null) {
                etDeliveryAddress.setError("Out of range (> 5 km radius limit)");
            }
            Toast.makeText(getContext(), "Pickup and delivery locations are " + String.format(Locale.getDefault(), "%.1f", distanceInKm) + " km apart. Maximum allowed radius is 5.0 km.", Toast.LENGTH_LONG).show();
            return;
        }

        String itemDesc = category + " Package";
        if (itemWeight.isEmpty()) itemWeight = "1.0 kg";

        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.createOrder(token, receiverPhone, pickupAddressStr, deliveryAddress, itemDesc, itemWeight, pLat, pLng, deliveryLat, deliveryLng, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Order created successfully within 5 km radius!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.action_createDelivery_to_orderConfirmation);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (error != null && (error.contains("token") || error.contains("auth") || error.contains("expired"))) {
                        Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                        UserSession.registerUser(requireContext(), "", "", "", "");
                        try {
                            Navigation.findNavController(v).navigate(R.id.action_global_to_login);
                        } catch (Exception ignored) {}
                    } else {
                        Toast.makeText(getContext(), "Failed to create order: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
