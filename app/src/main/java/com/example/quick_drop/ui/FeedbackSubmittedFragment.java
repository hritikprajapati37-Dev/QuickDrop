package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.appcompat.widget.Toolbar;
import com.example.quick_drop.R;

public class FeedbackSubmittedFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feedback_submitted, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_feedbackSubmitted_to_partnerDashboard));

        view.findViewById(R.id.btnHome).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_feedbackSubmitted_to_partnerDashboard));

        view.findViewById(R.id.btnHistory).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_feedbackSubmitted_to_partnerDashboard));
    }
}
