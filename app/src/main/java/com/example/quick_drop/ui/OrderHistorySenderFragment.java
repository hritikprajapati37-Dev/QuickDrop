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

public class OrderHistorySenderFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_history_sender, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.orderHistorySenderFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.senderDashboardFragment) {
                Navigation.findNavController(view).navigate(R.id.action_orderHistory_to_senderDashboard);
                return true;
            } else if (id == R.id.deliveriesFragment) {
                Navigation.findNavController(view).navigate(R.id.action_orderHistory_to_deliveries);
                return true;
            } else if (id == R.id.senderProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_orderHistory_to_senderProfile);
                return true;
            }
            return false;
        });

        // Set a click listener on the RecyclerView as a dummy for item clicks
        view.findViewById(R.id.rvOrderHistory).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_orderHistory_to_orderDetails));
    }
}
