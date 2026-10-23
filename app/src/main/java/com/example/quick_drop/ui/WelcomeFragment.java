package com.example.quick_drop.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.UserSession;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Auto-login persistent session check
        if (UserSession.isLoggedIn(requireContext())) {
            String role = UserSession.getUserRole(requireContext());
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded()) return;
                try {
                    if ("PARTNER".equalsIgnoreCase(role)) {
                        Navigation.findNavController(view).navigate(R.id.action_global_to_partnerDashboard);
                    } else {
                        Navigation.findNavController(view).navigate(R.id.action_global_to_senderDashboard);
                    }
                } catch (Exception ignored) {}
            }, 600);
            return;
        }

        view.findViewById(R.id.btnGetStarted).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_welcome_to_login));
    }
}
