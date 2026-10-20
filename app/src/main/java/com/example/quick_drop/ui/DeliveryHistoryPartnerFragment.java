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

public class DeliveryHistoryPartnerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_history_partner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.deliveryHistoryPartnerFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.partnerDashboardFragment) {
                Navigation.findNavController(view).navigate(R.id.action_history_to_partnerDashboard);
                return true;
            } else if (id == R.id.availableRequestsFragment) {
                Navigation.findNavController(view).navigate(R.id.action_history_to_availableRequests);
                return true;
            } else if (id == R.id.earningsFragment) {
                Navigation.findNavController(view).navigate(R.id.action_history_to_earnings);
                return true;
            } else if (id == R.id.partnerProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_history_to_partnerProfile);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.llHistory).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_history_to_details));
    }
}
