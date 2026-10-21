package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quick_drop.R;
import org.json.JSONObject;
import java.util.List;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private final List<JSONObject> orderList;

    public OrderHistoryAdapter(List<JSONObject> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history_sender, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject order = orderList.get(position);
        String orderId = order.optString("order_id", "");
        String shortId = orderId;
        if (shortId.length() > 6) shortId = shortId.substring(shortId.length() - 6);
        
        if (holder.tvOrderNum != null) holder.tvOrderNum.setText("#" + shortId);
        if (holder.tvStatusText != null) holder.tvStatusText.setText(order.optString("status", "CREATED"));
        if (holder.tvDestination != null) holder.tvDestination.setText(order.optString("delivery_address", ""));
        if (holder.tvPackageName != null) holder.tvPackageName.setText(order.optString("item_description", "Package"));
        if (holder.tvDateTime != null) holder.tvDateTime.setText("Recent");

        View.OnClickListener clickListener = v -> {
            Bundle args = new Bundle();
            args.putString("order_id", orderId);
            Navigation.findNavController(v).navigate(R.id.action_orderHistory_to_orderDetails, args);
        };

        holder.itemView.setOnClickListener(clickListener);
        if (holder.btnAction != null) {
            holder.btnAction.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNum, tvStatusText, tvDestination, tvPackageName, tvDateTime;
        View btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNum = itemView.findViewById(R.id.tvOrderNum);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
