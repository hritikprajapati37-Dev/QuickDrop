package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.quick_drop.utils.UserSession;
import android.widget.TextView;

public class PartnerDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvGreeting = view.findViewById(R.id.tvGreetingPartner);
        if (tvGreeting != null) {
            tvGreeting.setText("Good Morning, " + UserSession.getUserName(requireContext()).split(" ")[0]);
        }

        view.findViewById(R.id.btnViewDelivery).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_availableRequests));

        view.findViewById(R.id.btnViewRequests).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_availableRequests));

        view.findViewById(R.id.ivPartnerProfile).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_profile));

        View cardAddress = view.findViewById(R.id.cardAddressPartner);
        if (cardAddress != null) {
            cardAddress.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_partnerDashboard_to_partnerMap));
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.partnerDashboardFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.availableRequestsFragment) {
                Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_availableRequests);
                return true;
            } else if (id == R.id.deliveryHistoryPartnerFragment) {
                Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_history);
                return true;
            } else if (id == R.id.earningsFragment) {
                Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_earnings);
                return true;
            } else if (id == R.id.partnerProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_partnerDashboard_to_profile);
                return true;
            }
            return false;
        });
    }
}
