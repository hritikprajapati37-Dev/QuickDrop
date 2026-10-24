package com.example.quick_drop.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OrderDetailsFragment extends Fragment {

    private TextView tvOrderID, tvStatusBadge, tvPickupAddress, tvDestName, tvDestPhone, tvDropAddress, tvPartnerName, tvItemType, tvItemWeight, tvPaymentAmount, tvPaymentStatus;
    private TextView tvTime1, tvTime2, tvTime3, tvPickupOtpValue;
    private ImageView ivIcon1, ivIcon2, ivIcon3;
    private View cardPickupOtp;
    private MaterialButton btnFeedback, btnCopyOtp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());
        }

        tvOrderID = view.findViewById(R.id.tvOrderID);
        tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
        tvPickupAddress = view.findViewById(R.id.tvPickupAddress);
        tvDestName = view.findViewById(R.id.tvDestName);
        tvDestPhone = view.findViewById(R.id.tvDestPhone);
        tvDropAddress = view.findViewById(R.id.tvDropAddress);
        tvPartnerName = view.findViewById(R.id.tvPartnerName);
        tvItemType = view.findViewById(R.id.tvItemType);
        tvItemWeight = view.findViewById(R.id.tvItemWeight);
        tvPaymentAmount = view.findViewById(R.id.tvPaymentAmount);
        tvPaymentStatus = view.findViewById(R.id.tvPaymentStatus);

        tvTime1 = view.findViewById(R.id.tvTime1);
        tvTime2 = view.findViewById(R.id.tvTime2);
        tvTime3 = view.findViewById(R.id.tvTime3);

        ivIcon1 = view.findViewById(R.id.ivIcon1);
        ivIcon2 = view.findViewById(R.id.ivIcon2);
        ivIcon3 = view.findViewById(R.id.ivIcon3);

        cardPickupOtp = view.findViewById(R.id.cardPickupOtp);
        tvPickupOtpValue = view.findViewById(R.id.tvPickupOtp);
        btnCopyOtp = view.findViewById(R.id.btnCopyOtp);

        btnFeedback = view.findViewById(R.id.btnGiveFeedback);

        String orderId = getArguments() != null ? getArguments().getString("order_id", "") : "";
        loadOrderDetails(orderId);

        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("order_id", orderId);
                Navigation.findNavController(v).navigate(R.id.feedbackFragment, args);
            });
        }
    }

    private void loadOrderDetails(String orderId) {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        if (orderId != null && !orderId.isEmpty()) {
            ApiClient.getOrderDetails(token, orderId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject order) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> bindOrderData(order));
                }

                @Override
                public void onError(String error) {
                    loadLatestOrderFallback(token);
                }
            });
        } else {
            loadLatestOrderFallback(token);
        }
    }

    private void loadLatestOrderFallback(String token) {
        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    JSONArray orders = response.optJSONArray("orders");
                    if (orders != null && orders.length() > 0) {
                        JSONObject order = orders.optJSONObject(0);
                        if (order != null) {
                            bindOrderData(order);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void bindOrderData(JSONObject order) {
        if (order == null) return;

        String orderId = order.optString("order_id", "");
        String sId = orderId;
        if (sId.length() > 6) sId = sId.substring(sId.length() - 6);
        if (tvOrderID != null) tvOrderID.setText("Order #" + sId);

        String status = order.optString("status", "CREATED");
        String paymentStatus = order.optString("payment_status", "PENDING");
        boolean pickupVerified = order.optBoolean("pickup_verified", false);
        boolean delivered = order.optBoolean("delivered", false);

        if (tvStatusBadge != null) {
            if (delivered || "DELIVERED".equalsIgnoreCase(status)) {
                tvStatusBadge.setText("DELIVERED");
            } else if (pickupVerified || "PICKED_UP".equalsIgnoreCase(status) || "IN_TRANSIT".equalsIgnoreCase(status)) {
                tvStatusBadge.setText("OUT FOR DELIVERY");
            } else if ("ASSIGNED".equalsIgnoreCase(status) || "ARRIVED_PICKUP".equalsIgnoreCase(status)) {
                tvStatusBadge.setText("PARTNER ASSIGNED");
            } else if ("MATCHING".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(paymentStatus)) {
                tvStatusBadge.setText("SEARCHING PARTNER");
            } else if ("EXPIRED".equalsIgnoreCase(status) || "REFUNDED".equalsIgnoreCase(status) || "DRAFT_EXPIRED".equalsIgnoreCase(status)) {
                tvStatusBadge.setText("EXPIRED");
            } else {
                tvStatusBadge.setText(status);
            }
        }

        if (tvPickupAddress != null) tvPickupAddress.setText(order.optString("pickup_address", ""));
        if (tvDestName != null) tvDestName.setText("Receiver");
        if (tvDestPhone != null) tvDestPhone.setText(order.optString("receiver_phone", ""));
        if (tvDropAddress != null) tvDropAddress.setText(order.optString("delivery_address", ""));

        String partner = order.optString("assigned_partner", "");
        boolean hasAssignedPartner = !partner.isEmpty() && !"null".equalsIgnoreCase(partner) &&
                ("ASSIGNED".equalsIgnoreCase(status) || "ARRIVED_PICKUP".equalsIgnoreCase(status) ||
                 "PICKED_UP".equalsIgnoreCase(status) || "IN_TRANSIT".equalsIgnoreCase(status) ||
                 "DELIVERED".equalsIgnoreCase(status));

        if (tvPartnerName != null) {
            tvPartnerName.setText(hasAssignedPartner ? "Partner: " + partner : "Waiting for a delivery partner");
        }

        // Handle Pickup OTP display for Sender when pickup is pending
        String pickupOtp = order.optString("pickup_otp", "");
        if (pickupOtp.isEmpty() || "null".equalsIgnoreCase(pickupOtp)) {
            pickupOtp = order.optString("pickup_otp_plain", "");
        }

        boolean showOtpCard = !pickupVerified && !delivered &&
                !pickupOtp.isEmpty() && !"null".equalsIgnoreCase(pickupOtp) &&
                ("ASSIGNED".equalsIgnoreCase(status) || "ARRIVED_PICKUP".equalsIgnoreCase(status) ||
                 "MATCHING".equalsIgnoreCase(status) || "CREATED".equalsIgnoreCase(status));

        if (cardPickupOtp != null) {
            if (showOtpCard) {
                cardPickupOtp.setVisibility(View.VISIBLE);
                if (tvPickupOtpValue != null) {
                    if (pickupOtp.length() == 6) {
                        tvPickupOtpValue.setText(pickupOtp.substring(0, 3) + " " + pickupOtp.substring(3));
                    } else {
                        tvPickupOtpValue.setText(pickupOtp);
                    }
                }
                final String copyOtpVal = pickupOtp;
                if (btnCopyOtp != null) {
                    btnCopyOtp.setOnClickListener(v -> {
                        try {
                            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Pickup OTP", copyOtpVal);
                            if (clipboard != null) {
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(getContext(), "Pickup OTP copied: " + copyOtpVal, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "OTP: " + copyOtpVal, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                cardPickupOtp.setVisibility(View.GONE);
            }
        }

        if (tvItemType != null) tvItemType.setText(order.optString("item_description", "Package"));
        if (tvItemWeight != null) tvItemWeight.setText(order.optString("item_weight", "1.0 kg"));

        double totalAmount = order.optDouble("total_amount", 0.0);
        if (tvPaymentAmount != null) {
            tvPaymentAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
        }

        if (tvPaymentStatus != null) {
            tvPaymentStatus.setText(paymentStatus);
        }

        String orderTime = order.optString("order_time", order.optString("created_at", ""));
        String pickupTime = order.optString("pickup_time", "");
        String completeTime = order.optString("complete_order_time", "");

        if (tvTime1 != null) tvTime1.setText(formatStoredTime(orderTime));
        if (tvTime2 != null) tvTime2.setText(pickupVerified ? formatStoredTime(pickupTime) : "Pending");
        if (tvTime3 != null) tvTime3.setText(delivered ? formatStoredTime(completeTime) : "Pending");

        int checkedRes = R.drawable.ic_timeline_checked;
        int pendingRes = R.drawable.ic_timeline_pending;

        if (ivIcon1 != null) {
            ivIcon1.setImageResource("SUCCESS".equalsIgnoreCase(paymentStatus) || !"CREATED".equalsIgnoreCase(status) ? checkedRes : pendingRes);
        }
        if (ivIcon2 != null) {
            ivIcon2.setImageResource((pickupVerified || "PICKED_UP".equalsIgnoreCase(status) || "IN_TRANSIT".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)) ? checkedRes : pendingRes);
        }
        if (ivIcon3 != null) {
            ivIcon3.setImageResource((delivered || "DELIVERED".equalsIgnoreCase(status)) ? checkedRes : pendingRes);
        }

        if (btnFeedback != null) {
            btnFeedback.setVisibility((delivered || "DELIVERED".equalsIgnoreCase(status)) ? View.VISIBLE : View.GONE);
        }
    }

    private String formatStoredTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty() || "null".equals(isoTime)) {
            return "Pending";
        }
        try {
            String cleanIso = isoTime.replace("Z", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date;
            try {
                date = sdf.parse(cleanIso);
            } catch (Exception e) {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                date = sdf2.parse(cleanIso);
            }
            if (date != null) {
                SimpleDateFormat timeSdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                timeSdf.setTimeZone(TimeZone.getDefault());
                return timeSdf.format(date);
            }
        } catch (Exception ignored) {}
        return isoTime;
    }
}
