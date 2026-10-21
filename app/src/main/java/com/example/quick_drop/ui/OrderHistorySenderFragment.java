package com.example.quick_drop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quick_drop.R;
import com.example.quick_drop.utils.ApiClient;
import com.example.quick_drop.utils.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OrderHistorySenderFragment extends Fragment {

    private RecyclerView rvOrderHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_history_sender, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        rvOrderHistory = view.findViewById(R.id.rvOrderHistory);
        if (rvOrderHistory != null) {
            rvOrderHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        loadOrderHistory();

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.orderHistorySenderFragment);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.senderDashboardFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_orderHistory_to_senderDashboard);
                    return true;
                } else if (id == R.id.deliveriesFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_orderHistory_to_deliveries);
                    return true;
                } else if (id == R.id.senderProfileFragment) {
                    Navigation.findNavController(view).navigate(R.id.action_orderHistory_to_senderProfile);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadOrderHistory() {
        String token = UserSession.getToken(requireContext());
        if (token.isEmpty()) return;

        ApiClient.getOrdersList(token, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<JSONObject> list = new ArrayList<>();
                    JSONArray orders = response.optJSONArray("orders");
                    if (orders != null) {
                        for (int i = 0; i < orders.length(); i++) {
                            JSONObject obj = orders.optJSONObject(i);
                            if (obj != null) {
                                list.add(obj);
                            }
                        }
                    }
                    if (rvOrderHistory != null) {
                        rvOrderHistory.setAdapter(new OrderHistoryAdapter(list));
                    }
                });
            }

            @Override
            public void onError(String error) {}
        });
    }
}
