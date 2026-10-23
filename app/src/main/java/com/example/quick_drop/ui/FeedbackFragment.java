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
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import org.json.JSONObject;

public class FeedbackFragment extends Fragment {

    private String orderId = "";

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

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id", "");
        }

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        if (ratingBar != null) {
            ratingBar.setIsIndicator(false);
            ratingBar.setStepSize(1.0f);
        }

        EditText etReview = view.findViewById(R.id.etReview);

        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            float rating = ratingBar != null ? ratingBar.getRating() : 5.0f;
            String review = etReview != null ? etReview.getText().toString().trim() : "";
            String token = UserSession.getToken(requireContext());

            if (!token.isEmpty()) {
                ApiClient.submitFeedback(token, orderId, rating, review, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(v).navigate(R.id.action_feedback_to_submitted);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Submitted!", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(v).navigate(R.id.action_feedback_to_submitted);
                        });
                    }
                });
            } else {
                Navigation.findNavController(v).navigate(R.id.action_feedback_to_submitted);
            }
        });
    }
}
