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

import android.widget.Toast;

public class OrderTrackingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_tracking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnViewDetails).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_tracking_to_details));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.senderDashboardFragment) {
                Navigation.findNavController(view).navigate(R.id.action_tracking_to_senderDashboard);
                return true;
            } else if (id == R.id.deliveriesFragment) {
                Navigation.findNavController(view).navigate(R.id.action_tracking_to_deliveries);
                return true;
            } else if (id == R.id.orderHistorySenderFragment) {
                Navigation.findNavController(view).navigate(R.id.action_tracking_to_orderHistory);
                return true;
            } else if (id == R.id.senderProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_tracking_to_senderProfile);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnCall).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Calling Delivery Partner...", Toast.LENGTH_SHORT).show());
            
        view.findViewById(R.id.btnChat).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Chat feature coming soon!", Toast.LENGTH_SHORT).show());
    }
}
