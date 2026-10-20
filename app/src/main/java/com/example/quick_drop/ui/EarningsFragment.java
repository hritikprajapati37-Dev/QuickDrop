package com.example.quick_drop.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EarningsFragment extends Fragment {

    private TextView tvBalanceValue;
    private TextView tvDeliveriesValue;
    private TextView tvDistanceValue;
    private TextView tabToday, tabWeek, tabMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_earnings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        tvBalanceValue = view.findViewById(R.id.tvBalanceValue);
        tvDeliveriesValue = view.findViewById(R.id.tvDeliveriesValue);
        tvDistanceValue = view.findViewById(R.id.tvDistanceValue);
        tabToday = view.findViewById(R.id.tvTabToday);
        tabWeek = view.findViewById(R.id.tvTabWeek);
        tabMonth = view.findViewById(R.id.tvTabMonth);

        tabToday.setOnClickListener(v -> selectTab(0));
        tabWeek.setOnClickListener(v -> selectTab(1));
        tabMonth.setOnClickListener(v -> selectTab(2));

        view.findViewById(R.id.btnSeeAll).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_earnings_to_history));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.earningsFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.partnerDashboardFragment) {
                Navigation.findNavController(view).navigate(R.id.action_earnings_to_partnerDashboard);
                return true;
            } else if (id == R.id.availableRequestsFragment) {
                Navigation.findNavController(view).navigate(R.id.action_earnings_to_availableRequests);
                return true;
            } else if (id == R.id.deliveryHistoryPartnerFragment) {
                Navigation.findNavController(view).navigate(R.id.action_earnings_to_history);
                return true;
            } else if (id == R.id.earningsFragment) {
                return true;
            } else if (id == R.id.partnerProfileFragment) {
                Navigation.findNavController(view).navigate(R.id.action_earnings_to_partnerProfile);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnWithdraw).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Withdraw Funds")
                .setMessage("Your request to withdraw " + (tvBalanceValue != null ? tvBalanceValue.getText().toString() : "₹2,450") + " has been received and is being processed.")
                .setPositiveButton("OK", null)
                .show();
        });
    }

    private void selectTab(int index) {
        if (tabToday == null || tabWeek == null || tabMonth == null) return;
        
        tabToday.setBackground(null);
        tabToday.setBackgroundColor(Color.TRANSPARENT);
        tabToday.setTextColor(Color.parseColor("#6B7280"));

        tabWeek.setBackground(null);
        tabWeek.setBackgroundColor(Color.TRANSPARENT);
        tabWeek.setTextColor(Color.parseColor("#6B7280"));

        tabMonth.setBackground(null);
        tabMonth.setBackgroundColor(Color.TRANSPARENT);
        tabMonth.setTextColor(Color.parseColor("#6B7280"));

        if (index == 0) {
            tabToday.setBackgroundResource(R.drawable.shape_pill_white);
            tabToday.setTextColor(getResources().getColor(R.color.vf_primary));
            if (tvBalanceValue != null) tvBalanceValue.setText("₹2,450");
            if (tvDeliveriesValue != null) tvDeliveriesValue.setText("12");
            if (tvDistanceValue != null) tvDistanceValue.setText("45");
        } else if (index == 1) {
            tabWeek.setBackgroundResource(R.drawable.shape_pill_white);
            tabWeek.setTextColor(getResources().getColor(R.color.vf_primary));
            if (tvBalanceValue != null) tvBalanceValue.setText("₹14,800");
            if (tvDeliveriesValue != null) tvDeliveriesValue.setText("78");
            if (tvDistanceValue != null) tvDistanceValue.setText("310");
        } else {
            tabMonth.setBackgroundResource(R.drawable.shape_pill_white);
            tabMonth.setTextColor(getResources().getColor(R.color.vf_primary));
            if (tvBalanceValue != null) tvBalanceValue.setText("₹58,200");
            if (tvDeliveriesValue != null) tvDeliveriesValue.setText("320");
            if (tvDistanceValue != null) tvDistanceValue.setText("1,240");
        }
    }
}
