package com.example.quick_drop.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;

public class ProofOfDeliveryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_proof_of_delivery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        View btnCall = view.findViewById(R.id.btnCall);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:9876543210"));
                startActivity(intent);
            });
        }

        EditText etNotes = view.findViewById(R.id.etDeliveryNotes);

        view.findViewById(R.id.btnComplete).setOnClickListener(v -> {
            String notes = etNotes != null ? etNotes.getText().toString() : "";
            Toast.makeText(getContext(), "Delivery completed with notes: " + notes, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigate(R.id.action_proofOfDelivery_to_completed);
        });
    }
}
