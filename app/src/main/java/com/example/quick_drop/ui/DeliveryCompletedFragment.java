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

public class DeliveryCompletedFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_completed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnDashboard).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_completed_to_dashboard));

        view.findViewById(R.id.btnEarnings).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_completed_to_earnings));

        // Feedback navigation
        View successArea = view.findViewById(R.id.successArea);
        if (successArea != null) {
            successArea.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_completed_to_feedback));
        }
            
        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_completed_to_dashboard));
    }
}
