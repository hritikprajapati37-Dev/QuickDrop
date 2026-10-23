package com.example.quick_drop.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import org.json.JSONObject;

public class EditPartnerProfileFragment extends Fragment {

    private EditText etFullName, etPhone, etEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_partner_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etFullName = view.findViewById(R.id.etFullName);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);

        if (etPhone != null) etPhone.setEnabled(false);

        loadProfileData();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnSave).setOnClickListener(this::saveProfile);
    }

    private void loadProfileData() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getProfile(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    String name = response.optString("name", "");
                    String phone = response.optString("phone", "");
                    String email = response.optString("email", "");

                    if (etFullName != null) etFullName.setText(name);
                    if (etPhone != null) etPhone.setText(phone);
                    if (etEmail != null) etEmail.setText(email);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (error != null && (error.contains("404") || error.contains("User not found") || error.contains("Invalid authentication token"))) {
                        UserSession.clearSession(requireContext());
                        Toast.makeText(getContext(), "Account session expired or invalid. Please log in.", Toast.LENGTH_LONG).show();
                        try {
                            Navigation.findNavController(requireView()).navigate(R.id.action_global_to_login);
                        } catch (Exception ignored) {}
                    } else {
                        if (etFullName != null) etFullName.setText(UserSession.getUserName(requireContext()));
                        if (etPhone != null) etPhone.setText(UserSession.getUserPhone(requireContext()));
                    }
                });
            }
        });
    }

    private void saveProfile(View v) {
        String name = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (name.isEmpty()) {
            if (etFullName != null) etFullName.setError("Name is required");
            return;
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (etEmail != null) etEmail.setError("Invalid email format");
            Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Session expired. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.updateProfile(token, name, email, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Profile updated successfully in database!", Toast.LENGTH_SHORT).show();
                    UserSession.registerUser(requireContext(), name, UserSession.getUserPhone(requireContext()), UserSession.getUserRole(requireContext()), token);
                    Navigation.findNavController(v).navigateUp();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (error != null && (error.contains("404") || error.contains("User not found") || error.contains("Invalid authentication token"))) {
                        UserSession.clearSession(requireContext());
                        Toast.makeText(getContext(), "Account session expired or invalid. Please log in.", Toast.LENGTH_LONG).show();
                        try {
                            Navigation.findNavController(requireView()).navigate(R.id.action_global_to_login);
                        } catch (Exception ignored) {}
                    } else {
                        Toast.makeText(getContext(), "Failed to update profile: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
