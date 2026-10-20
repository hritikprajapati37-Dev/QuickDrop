package com.example.quick_drop.utils;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {
    // Production Render Backend URL over HTTPS
    private static final String PRODUCTION_URL = "https://quickdrop-bhd9.onrender.com";

    // Local development configuration (Set USE_LOCAL_DEV to true when running Flask locally)
    private static final boolean USE_LOCAL_DEV = false;
    private static final String LOCAL_PC_LAN_IP = "192.168.0.101";

    private static String getBaseUrl() {
        if (USE_LOCAL_DEV) {
            if (isEmulator()) {
                return "http://10.0.2.2:5000";
            } else {
                return "http://" + LOCAL_PC_LAN_IP + ":5000";
            }
        }
        return PRODUCTION_URL;
    }

    private static boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public static void signup(String name, String phone, String password, String role, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(getBaseUrl() + "/api/auth/signup");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
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
                    callback.onError(resJson.optString("error", "Signup failed"));
                }
            } catch (Exception e) {
                Log.e("ApiClient", "Signup error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void login(String phone, String password, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(getBaseUrl() + "/api/auth/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("phone", phone);
                jsonParam.put("password", password);

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
                    callback.onError(resJson.optString("error", "Login failed"));
                }
            } catch (Exception e) {
                Log.e("ApiClient", "Login error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void getDashboard(String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(getBaseUrl() + "/api/user/dashboard");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");

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
                    callback.onError(resJson.optString("error", "Failed to load dashboard"));
                }
            } catch (Exception e) {
                Log.e("ApiClient", "Dashboard error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void getOrdersList(String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(getBaseUrl() + "/api/orders/list");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/json");

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
                    callback.onError(resJson.optString("error", "Failed to fetch orders"));
                }
            } catch (Exception e) {
                Log.e("ApiClient", "Orders list error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
