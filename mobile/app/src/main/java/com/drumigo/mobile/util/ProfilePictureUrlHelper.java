package com.drumigo.mobile.util;

import androidx.annotation.Nullable;

/**
 * Resolves relative profile picture URLs from the backend to absolute URLs for display.
 * Backend may return paths like "/uploads/profile/xyz.jpg"; we prepend the server origin.
 */
public final class ProfilePictureUrlHelper {

    private ProfilePictureUrlHelper() {
    }

    /**
     * @param url       Value from ProfileResponse.profilePictureUrl (may be null or empty).
     * @param apiBaseUrl BuildConfig.API_BASE_URL (e.g. "http://localhost:8080/api" or "http://10.0.2.2:8080/api").
     * @return Absolute URL for loading the image, or null if url is null/empty or invalid.
     */
    @Nullable
    public static String resolveProfilePictureUrl(@Nullable String url, @Nullable String apiBaseUrl) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            return url;
        }
        String origin = toServerOrigin(apiBaseUrl);
        if (origin == null) {
            return null;
        }
        String path = url.startsWith("/") ? url : "/" + url;
        return origin + path;
    }

    /**
     * Strips /api or /api/ from API base URL to get server origin (e.g. http://localhost:8080).
     */
    @Nullable
    private static String toServerOrigin(@Nullable String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.trim().isEmpty()) {
            return null;
        }
        String base = apiBaseUrl.trim();
        if (base.endsWith("/api")) {
            return base.substring(0, base.length() - 4);
        }
        if (base.endsWith("/api/")) {
            return base.substring(0, base.length() - 5);
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
