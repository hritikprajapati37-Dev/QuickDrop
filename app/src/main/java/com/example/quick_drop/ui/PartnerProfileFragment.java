package com.example.quick_drop.ui;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONObject;

public class PartnerProfileFragment extends Fragment {

    private TextView tvName, tvPhone, tvEmail;
    private View btnAddEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvName);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnAddEmail = view.findViewById(R.id.btnAddEmail);

        if (tvName != null) {
            tvName.setText(UserSession.getUserName(requireContext()));
        }
        if (tvPhone != null) {
            tvPhone.setText(UserSession.getUserPhone(requireContext()));
        }

        loadProfile();

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        View btnEditProfile = view.findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_partnerProfile_to_editPartnerProfile));
        }

        if (btnAddEmail != null) {
            btnAddEmail.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_partnerProfile_to_editPartnerProfile));
        }

        View btnTerms = view.findViewById(R.id.btnTerms);
        if (btnTerms != null) {
            btnTerms.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_partnerProfile_to_tos));
        }

        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                UserSession.clearSession(requireContext());
                Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(v).navigate(R.id.action_global_to_login);
            });
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.partnerProfileFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.partnerDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerProfile_to_partnerDashboard);
                    return true;
                } else if (id == R.id.availableRequestsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerProfile_to_availableRequests);
                    return true;
                } else if (id == R.id.deliveryHistoryPartnerFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerProfile_to_history);
                    return true;
                } else if (id == R.id.earningsFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_partnerProfile_to_earnings);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
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

                    if (tvName != null && !name.isEmpty()) tvName.setText(name);
                    if (tvPhone != null && !phone.isEmpty()) tvPhone.setText(phone);
                    if (tvEmail != null) {
                        if (!email.isEmpty() && !"null".equals(email)) {
                            tvEmail.setText(email);
                            tvEmail.setTextColor(getResources().getColor(R.color.vf_on_surface, null));
                            tvEmail.setTypeface(null, android.graphics.Typeface.BOLD);
                            if (btnAddEmail != null) btnAddEmail.setVisibility(View.GONE);
                        } else {
                            tvEmail.setText("Not added");
                            tvEmail.setTextColor(getResources().getColor(R.color.vf_outline, null));
                            if (btnAddEmail != null) btnAddEmail.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }
}
