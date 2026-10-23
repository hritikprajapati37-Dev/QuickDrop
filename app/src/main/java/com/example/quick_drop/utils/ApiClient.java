package com.example.quick_drop.utils;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class ApiClient {
    private static final String TAG = "ApiClient";

    // Production Render Backend URL over HTTPS (Authoritative Deployed Environment)
    private static final String PRODUCTION_URL = "https://quickdrop-bhd9.onrender.com";

    // Local development configuration (Set USE_LOCAL_DEV to true only when running local Flask instance)
    private static final boolean USE_LOCAL_DEV = false;
    private static final String LOCAL_PC_LAN_IP = "10.0.2.2"; // Emulator loopback alias

    // Extended timeout (45 seconds) to accommodate Render free-tier cold starts (spin-down wakeup)
    private static final int TIMEOUT_MS = 45000;

    private static String getBaseUrl() {
        if (USE_LOCAL_DEV) {
            return "http://" + LOCAL_PC_LAN_IP + ":5000";
        }
        return PRODUCTION_URL;
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    private static String handleException(Exception e, String endpoint) {
        String excClass = e.getClass().getSimpleName();
        String msg = e.getMessage();
        Log.e(TAG, "Request Exception [" + excClass + "] for endpoint: " + endpoint + " -> " + msg, e);

        if (e instanceof SocketTimeoutException) {
            return "Server is starting up from cold start (Render). Please tap retry in a few seconds. (" + excClass + ")";
        }
        if (msg != null && (msg.contains("Unable to resolve host") || msg.contains("failed to connect") || msg.contains("Connection refused"))) {
            return "Network connection error (" + excClass + ": " + msg + "). Check internet connection.";
        }
        return excClass + ": " + (msg != null ? msg : "Unknown network error");
    }

    public static void signup(String name, String phone, String password, String role, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/auth/signup";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                Log.d(TAG, "Executing POST " + url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("name", name);
                jsonParam.put("phone", phone);
                jsonParam.put("password", password);
                jsonParam.put("role", role);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                Log.d(TAG, "Response Code " + code + " for POST " + endpoint);
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Signup failed (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void login(String phone, String password, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/auth/login";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                Log.d(TAG, "Executing POST " + url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("phone", phone);
                jsonParam.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                Log.d(TAG, "Response Code " + code + " for POST " + endpoint);
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Login failed (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void getDashboard(String token, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/user/dashboard";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to load dashboard (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void getOrdersList(String token, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/orders/list";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to fetch orders (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void getOrderDetails(String token, String orderId, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/orders/" + orderId;
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to fetch order details (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void getAvailableRequests(String token, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/partner/available";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to fetch available requests (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void acceptRequest(String token, String orderId, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/delivery/accept";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("order_id", orderId);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to accept order (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void createOrder(String token, String receiverPhone, String pickupAddress, String deliveryAddress, String itemDescription, String itemWeight, double pickupLat, double pickupLng, double deliveryLat, double deliveryLng, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/orders";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("receiver_phone", receiverPhone);
                jsonParam.put("pickup_address", pickupAddress);
                jsonParam.put("delivery_address", deliveryAddress);
                jsonParam.put("item_description", itemDescription);
                jsonParam.put("item_weight", itemWeight);
                jsonParam.put("pickup_lat", pickupLat);
                jsonParam.put("pickup_lng", pickupLng);
                jsonParam.put("delivery_lat", deliveryLat);
                jsonParam.put("delivery_lng", deliveryLng);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Order creation failed (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void payOrder(String token, String orderId, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/orders/" + orderId + "/pay";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Payment failed (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void getPartnerEarnings(String token, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/partner/earnings";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to fetch earnings (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void getProfile(String token, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/user/profile";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to load profile (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }

    public static void updateProfile(String token, String name, String email, ApiCallback callback) {
        new Thread(() -> {
            String endpoint = "/api/user/profile";
            try {
                URL url = new URL(getBaseUrl() + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("name", name);
                jsonParam.put("email", email);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject resJson = new JSONObject(response.toString());
                if (code >= 200 && code < 300) {
                    callback.onSuccess(resJson);
                } else {
                    callback.onError(resJson.optString("error", "Failed to update profile (HTTP " + code + ")"));
                }
            } catch (Exception e) {
                callback.onError(handleException(e, endpoint));
            }
        }).start();
    }
}
