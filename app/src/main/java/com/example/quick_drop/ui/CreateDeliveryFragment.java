package com.example.quick_drop.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class CreateDeliveryFragment extends Fragment {

    private TextInputEditText etReceiverName, etReceiverPhone, etDeliveryAddress, etItemName;
    private AutoCompleteTextView actvCategory;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 12.9716;
    private double currentLng = 77.5946;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_delivery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        fetchLocation();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        String[] categories = new String[] {"Documents", "Electronics", "Food", "Fragile", "Clothing", "General"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory = view.findViewById(R.id.actvCategory);
        if (actvCategory != null) {
            actvCategory.setAdapter(adapter);
        }

        etReceiverName = view.findViewById(R.id.etReceiverName);
        etReceiverPhone = view.findViewById(R.id.etReceiverPhone);
        etDeliveryAddress = view.findViewById(R.id.etDeliveryAddress);
        etItemName = view.findViewById(R.id.etItemName);

        view.findViewById(R.id.btnNext).setOnClickListener(v -> submitOrder(v));
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                }
            });
        }
    }

    private void submitOrder(View v) {
        String receiverName = etReceiverName != null && etReceiverName.getText() != null ? etReceiverName.getText().toString().trim() : "";
        String receiverPhone = etReceiverPhone != null && etReceiverPhone.getText() != null ? etReceiverPhone.getText().toString().trim() : "";
        String deliveryAddress = etDeliveryAddress != null && etDeliveryAddress.getText() != null ? etDeliveryAddress.getText().toString().trim() : "";
        String itemName = etItemName != null && etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
        String category = actvCategory != null ? actvCategory.getText().toString().trim() : "General";

        if (receiverPhone.isEmpty() || deliveryAddress.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in receiver phone and delivery address", Toast.LENGTH_SHORT).show();
            return;
        }

        String itemDesc = (!itemName.isEmpty() ? itemName : "Package") + " (" + category + ")";
        String pickupAddress = "123 Tech Park, Sharma Heights, Bengaluru";

        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.createOrder(token, receiverPhone, pickupAddress, deliveryAddress, itemDesc, currentLat, currentLng, currentLat + 0.02, currentLng + 0.02, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Order created successfully!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.action_createDelivery_to_orderConfirmation);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to create order: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
