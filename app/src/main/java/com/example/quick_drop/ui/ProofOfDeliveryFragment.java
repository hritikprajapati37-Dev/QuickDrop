package com.example.quick_drop.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class ProofOfDeliveryFragment extends Fragment {

    private TextView tvHeaderOrderNum, tvReceiverName, tvReceiverPhone, tvSummaryDesc;
    private TextView btnHandedRecipient, btnHandedDoor, btnHandedReception;
    private TextInputEditText etDeliveryOtp;
    private EditText etDeliveryNotes;
    private MaterialButton btnComplete, btnArrivedDestination, btnCall;
    private View deliveryOtpCard, handedToCard, deliveryNotesCard;
    private String currentOrderId = "";
    private String receiverPhoneStr = "";
    private String selectedHandedTo = "Recipient";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_proof_of_delivery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvHeaderOrderNum = view.findViewById(R.id.tvHeaderOrderNum);
        tvReceiverName = view.findViewById(R.id.tvReceiverName);
        tvReceiverPhone = view.findViewById(R.id.tvReceiverPhone);
        tvSummaryDesc = view.findViewById(R.id.tvSummaryDesc);
        btnCall = view.findViewById(R.id.btnCall);

        etDeliveryOtp = view.findViewById(R.id.etDeliveryOtp);
        etDeliveryNotes = view.findViewById(R.id.etDeliveryNotes);
        btnComplete = view.findViewById(R.id.btnComplete);
        btnArrivedDestination = view.findViewById(R.id.btnArrivedDestination);
        deliveryOtpCard = view.findViewById(R.id.deliveryOtpCard);
        handedToCard = view.findViewById(R.id.handedToCard);
        deliveryNotesCard = view.findViewById(R.id.deliveryNotesCard);

        btnHandedRecipient = view.findViewById(R.id.btnHandedRecipient);
        btnHandedDoor = view.findViewById(R.id.btnHandedDoor);
        btnHandedReception = view.findViewById(R.id.btnHandedReception);

        if (getArguments() != null) {
            currentOrderId = getArguments().getString("order_id", "");
        }

        loadOrderData();

        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                if (!receiverPhoneStr.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + receiverPhoneStr));
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Receiver phone number unavailable", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnArrivedDestination != null) {
            btnArrivedDestination.setOnClickListener(v -> markArrivedDestination());
        }

        setupHandedToToggles();

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> confirmDelivery());
        }
    }

    private void setupHandedToToggles() {
        View.OnClickListener listener = v -> {
            int id = v.getId();
            if (id == R.id.btnHandedRecipient) {
                selectedHandedTo = "Recipient";
            } else if (id == R.id.btnHandedDoor) {
                selectedHandedTo = "Door";
            } else if (id == R.id.btnHandedReception) {
                selectedHandedTo = "Reception";
            }

            if (btnHandedRecipient != null) {
                btnHandedRecipient.setBackgroundResource(R.drawable.shape_rounded_md);
                btnHandedRecipient.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        id == R.id.btnHandedRecipient ? getResources().getColor(R.color.vf_primary) : getResources().getColor(R.color.white)));
                btnHandedRecipient.setTextColor(id == R.id.btnHandedRecipient ? getResources().getColor(R.color.white) : getResources().getColor(R.color.vf_on_surface_variant));
            }
            if (btnHandedDoor != null) {
                btnHandedDoor.setBackgroundResource(R.drawable.shape_rounded_md);
                btnHandedDoor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        id == R.id.btnHandedDoor ? getResources().getColor(R.color.vf_primary) : getResources().getColor(R.color.white)));
                btnHandedDoor.setTextColor(id == R.id.btnHandedDoor ? getResources().getColor(R.color.white) : getResources().getColor(R.color.vf_on_surface_variant));
            }
            if (btnHandedReception != null) {
                btnHandedReception.setBackgroundResource(R.drawable.shape_rounded_md);
                btnHandedReception.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        id == R.id.btnHandedReception ? getResources().getColor(R.color.vf_primary) : getResources().getColor(R.color.white)));
                btnHandedReception.setTextColor(id == R.id.btnHandedReception ? getResources().getColor(R.color.white) : getResources().getColor(R.color.vf_on_surface_variant));
            }
        };

        if (btnHandedRecipient != null) btnHandedRecipient.setOnClickListener(listener);
        if (btnHandedDoor != null) btnHandedDoor.setOnClickListener(listener);
        if (btnHandedReception != null) btnHandedReception.setOnClickListener(listener);
    }

    private void loadOrderData() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty() || currentOrderId.isEmpty()) return;

        ApiClient.getOrderDetails(token, currentOrderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject order) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    String shortId = currentOrderId;
                    if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
                    if (tvHeaderOrderNum != null) tvHeaderOrderNum.setText("#" + shortId);

                    receiverPhoneStr = order.optString("receiver_phone", "");
                    if (tvReceiverPhone != null) tvReceiverPhone.setText(receiverPhoneStr);
                    if (tvReceiverName != null) tvReceiverName.setText("Receiver (" + receiverPhoneStr + ")");

                    String itemDesc = order.optString("item_description", "Package");
                    String itemWeight = order.optString("item_weight", "1.0 kg");
                    if (tvSummaryDesc != null) tvSummaryDesc.setText(itemDesc + "  " + itemWeight);

                    String status = order.optString("status", "");
                    updateDeliveryActions(status);
                    if (!"ARRIVED_AT_DESTINATION".equalsIgnoreCase(status) && !"ARRIVED_DESTINATION".equalsIgnoreCase(status)) {
                        markArrivedDestinationSilent();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Unable to load delivery: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void updateDeliveryActions(String status) {
        if (btnArrivedDestination != null) {
            btnArrivedDestination.setVisibility(View.GONE);
        }
        if (deliveryOtpCard != null) deliveryOtpCard.setVisibility(View.VISIBLE);
        if (handedToCard != null) handedToCard.setVisibility(View.VISIBLE);
        if (deliveryNotesCard != null) deliveryNotesCard.setVisibility(View.VISIBLE);
        if (btnComplete != null) btnComplete.setVisibility(View.VISIBLE);
    }

    private void markArrivedDestinationSilent() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty() || currentOrderId.isEmpty()) return;

        ApiClient.arrivedDestination(token, currentOrderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {}

            @Override
            public void onError(String error) {}
        });
    }

    private void markArrivedDestination() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty() || currentOrderId.isEmpty()) return;

        ApiClient.arrivedDestination(token, currentOrderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Arrived at destination recorded!", Toast.LENGTH_SHORT).show();
                    loadOrderData();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Arrived update failed: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void confirmDelivery() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty() || currentOrderId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid session or order ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = etDeliveryOtp != null && etDeliveryOtp.getText() != null ? etDeliveryOtp.getText().toString().trim() : "";
        if (otp.length() != 6) {
            if (etDeliveryOtp != null) etDeliveryOtp.setError("Enter 6-digit Delivery OTP");
            Toast.makeText(getContext(), "Please enter the 6-digit Delivery OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnComplete != null) {
            btnComplete.setEnabled(false);
            btnComplete.setText("Confirming Delivery...");
        }

        ApiClient.verifyDelivery(token, currentOrderId, otp, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Delivery confirmed successfully!", Toast.LENGTH_SHORT).show();
                    try {
                        Navigation.findNavController(requireView()).navigate(R.id.action_proofOfDelivery_to_completed);
                    } catch (Exception e) {
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (btnComplete != null) {
                        btnComplete.setEnabled(true);
                        btnComplete.setText("Confirm Delivery");
                    }
                    Toast.makeText(getContext(), "Delivery verification failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
