package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;

public class FeedbackFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        if (ratingBar != null) {
            ratingBar.setIsIndicator(false);
            ratingBar.setStepSize(1.0f);
        }

        EditText etReview = view.findViewById(R.id.etReview);

        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            float rating = ratingBar != null ? ratingBar.getRating() : 5.0f;
            String review = etReview != null ? etReview.getText().toString() : "";
            Toast.makeText(getContext(), "Rating: " + rating + " submitted successfully!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigate(R.id.action_feedback_to_submitted);
        });
    }
}
