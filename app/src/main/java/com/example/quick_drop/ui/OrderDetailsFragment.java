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
import com.google.android.material.button.MaterialButton;

public class OrderDetailsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());
            
        view.findViewById(R.id.btnLiveTrack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_global_to_partnerDashboard)); // Just for demo or if it's supposed to be feedback
        
        // Fix the button that says "Give Feedback" but navigates to tracking
        MaterialButton btnFeedback = view.findViewById(R.id.btnLiveTrack);
        if (btnFeedback != null && btnFeedback.getText().toString().equalsIgnoreCase("Give Feedback")) {
            btnFeedback.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.feedbackFragment));
        }
    }
}
