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

public class DeliveriesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_deliveries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.deliveriesFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.senderDashboardFragment) {
                Navigation.findNavController(view).navigate(R.id.action_deliveries_to_senderDashboard);
                return true;
            } else if (id == R.id.orderHistorySenderFragment) {
                Navigation.findNavController(view).navigate(R.id.action_deliveries_to_orderHistory);
                return true;
            } else if (id == R.id.senderProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_deliveries_to_senderProfile);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnTrack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_deliveries_to_tracking));
        
        view.findViewById(R.id.btnDetails).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_deliveries_to_orderDetails));
    }
}
