package com.example.quick_drop.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class UserSession {
    private static final String PREF_NAME = "SecureQuickDropPrefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_TOKEN = "auth_token";

    private static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static void registerUser(Context context, String name, String phone, String role) {
        registerUser(context, name, phone, role, "");
    }

    public static void registerUser(Context context, String name, String phone, String role, String token) {
        SharedPreferences.Editor editor = getEncryptedPrefs(context).edit();
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public static void clearSession(Context context) {
        SharedPreferences.Editor editor = getEncryptedPrefs(context).edit();
        editor.clear();
        editor.apply();
    }

    public static String getToken(Context context) {
        return getEncryptedPrefs(context).getString(KEY_TOKEN, "");
    }

    public static String getUserName(Context context) {
        return getEncryptedPrefs(context).getString(KEY_NAME, "User");
    }

    public static String getUserPhone(Context context) {
        return getEncryptedPrefs(context).getString(KEY_PHONE, "");
    }

    public static String getUserRole(Context context) {
        return getEncryptedPrefs(context).getString(KEY_ROLE, "SENDER");
    }

    public static boolean isRegistered(Context context) {
        return !getEncryptedPrefs(context).getString(KEY_PHONE, "").isEmpty();
    }

    public static boolean isLoggedIn(Context context) {
        String token = getToken(context);
        return token != null && !token.isEmpty();
    }
}
