package com.drumigo.mobile.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public final class SessionManager {

    private static final String PREFS_NAME = "auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_SESSION_EXPIRED_NOTICE = "sessionExpiredNotice";

    private static volatile SessionManager instance;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    public void saveSession(String token, long userId, String email, String role) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_SESSION_EXPIRED_NOTICE, false)
            .apply();
    }

    public void clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_ROLE)
            .apply();
    }

    public void markSessionExpired() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_ROLE)
            .putBoolean(KEY_SESSION_EXPIRED_NOTICE, true)
            .apply();
    }

    public boolean consumeSessionExpiredNotice() {
        boolean shouldShow = prefs.getBoolean(KEY_SESSION_EXPIRED_NOTICE, false);
        if (shouldShow) {
            prefs.edit().putBoolean(KEY_SESSION_EXPIRED_NOTICE, false).apply();
        }
        return shouldShow;
    }

    @Nullable
    public String getToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        if (isTokenExpired(token)) {
            clearSession();
            return null;
        }
        return token;
    }

    public boolean isAuthenticated() {
        return getToken() != null;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    @Nullable
    public String getRole() {
        String storedRole = prefs.getString(KEY_ROLE, null);
        if (storedRole != null && !storedRole.trim().isEmpty()) {
            return storedRole;
        }
        JSONObject payload = getTokenPayload();
        return payload == null ? null : payload.optString("role", null);
    }

    @Nullable
    public String getEmail() {
        String storedEmail = prefs.getString(KEY_EMAIL, null);
        if (storedEmail != null && !storedEmail.trim().isEmpty()) {
            return storedEmail;
        }
        JSONObject payload = getTokenPayload();
        return payload == null ? null : payload.optString("sub", null);
    }

    private boolean isTokenExpired(String token) {
        try {
            JSONObject payload = decodeTokenPayload(token);
            long exp = payload.optLong("exp", -1L);
            if (exp <= 0L) {
                return true;
            }
            long nowEpochSeconds = System.currentTimeMillis() / 1000L;
            return exp <= nowEpochSeconds;
        } catch (Exception ignored) {
            return true;
        }
    }

    @Nullable
    private JSONObject getTokenPayload() {
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            if (isTokenExpired(token)) {
                clearSession();
                return null;
            }
            return decodeTokenPayload(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONObject decodeTokenPayload(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return new JSONObject(new String(decoded));
    }
}
