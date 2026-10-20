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
import com.example.quick_drop.utils.UserSession;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etFullName = view.findViewById(R.id.etFullName);
        TextInputEditText etPhone = view.findViewById(R.id.etPhone);

        if (etFullName != null) {
            etFullName.setText(UserSession.getUserName(requireContext()));
        }
        if (etPhone != null) {
            etPhone.setText(UserSession.getUserPhone(requireContext()));
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            if (etFullName != null && etFullName.getText() != null) {
                String newName = etFullName.getText().toString().trim();
                if (!newName.isEmpty()) {
                    UserSession.registerUser(requireContext(), newName, UserSession.getUserPhone(requireContext()), UserSession.getUserRole(requireContext()), UserSession.getToken(requireContext()));
                }
            }
            Navigation.findNavController(v).navigateUp();
        });
    }
}
