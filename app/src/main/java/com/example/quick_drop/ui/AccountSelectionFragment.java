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

public class AccountSelectionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Sender selection card
        view.findViewById(R.id.cardSender).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_selection_to_senderSignup));

        // Partner selection card
        view.findViewById(R.id.cardPartner).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_selection_to_partnerSignup));

        // Back to login footer
        view.findViewById(R.id.footerLayout).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());
    }
}
