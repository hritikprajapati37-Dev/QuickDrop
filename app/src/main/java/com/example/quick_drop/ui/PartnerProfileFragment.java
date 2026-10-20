package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.quick_drop.utils.UserSession;
import android.widget.TextView;

public class PartnerProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.tvName);
        if (tvName != null) {
            tvName.setText(UserSession.getUserName(requireContext()));
        }

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerProfile_to_editPartnerProfile));

        view.findViewById(R.id.btnTerms).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerProfile_to_tos));

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_to_login));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
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
