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

public class SenderProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sender_profile, container, false);
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
            Navigation.findNavController(v).navigate(R.id.action_senderProfile_to_editProfile));

        view.findViewById(R.id.btnTermsPrivacy).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_senderProfile_to_tos));

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_to_login));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.senderProfileFragment);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.senderDashboardFragment) {
                Navigation.findNavController(view).navigate(R.id.action_senderProfile_to_senderDashboard);
                return true;
            } else if (id == R.id.deliveriesFragment) {
                Navigation.findNavController(view).navigate(R.id.action_senderProfile_to_deliveries);
                return true;
            } else if (id == R.id.orderHistorySenderFragment) {
                Navigation.findNavController(view).navigate(R.id.action_senderProfile_to_orderHistory);
                return true;
            }
            return false;
        });
    }
}
