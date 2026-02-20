package com.drumigo.mobile.ui.history;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.drumigo.mobile.BuildConfig;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.MapboxApiClient;
import com.drumigo.mobile.data.api.MapboxDirectionsService;
import com.drumigo.mobile.data.api.MapboxGeocodingService;
import com.drumigo.mobile.data.model.Ride;
import com.drumigo.mobile.data.model.mapbox.MapboxDirectionsResponse;
import com.drumigo.mobile.data.model.mapbox.MapboxGeocodingResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationsUtils;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions;
import com.mapbox.maps.plugin.attribution.AttributionUtils;
import com.mapbox.maps.plugin.logo.LogoUtils;
import com.mapbox.maps.plugin.scalebar.ScaleBarUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RideHistoryRouteMapDialog {

    private static final double DEFAULT_LNG = 19.8200;
    private static final double DEFAULT_LAT = 45.2500;
    private static final double DEFAULT_ZOOM = 12.2;
    private static final String MAPBOX_STYLE_URI = "mapbox://styles/mapbox/streets-v12";
    private static final long CAMERA_ANIMATION_MS = 900L;
    private static final List<Double> ROUTE_LABEL_ICON_OFFSET = Arrays.asList(0.0, -2.3);

    private final Context context;
    private final Ride ride;
    private final MapboxGeocodingService geocodingService;
    private final MapboxDirectionsService directionsService;

    private BottomSheetDialog dialog;
    private MapView mapView;
    private TextView statusText;
    private ProgressBar progressBar;
    private View overlay;

    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private ValueAnimator cameraAnimator;
    private int routeRequestToken = 0;
    private boolean dismissed = false;

    private RideHistoryRouteMapDialog(@NonNull Context context, @NonNull Ride ride) {
        this.context = context;
        this.ride = ride;
        this.geocodingService = MapboxApiClient.getGeocodingService();
        this.directionsService = MapboxApiClient.getDirectionsService();
    }

    static void show(@NonNull Context context, @NonNull Ride ride) {
        new RideHistoryRouteMapDialog(context, ride).showInternal();
    }

    private void showInternal() {
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.dialog_ride_history_route_map);
        dialog.setDismissWithAnimation(true);

        mapView = dialog.findViewById(R.id.rideRouteMapView);
        statusText = dialog.findViewById(R.id.rideRouteMapStatusText);
        progressBar = dialog.findViewById(R.id.rideRouteMapProgress);
        overlay = dialog.findViewById(R.id.rideRouteMapOverlay);
        ImageView closeButton = dialog.findViewById(R.id.rideRouteMapCloseButton);

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        attachLifecycleOwnerIfPossible();

        dialog.setOnShowListener(ignored -> configureBottomSheet());
        dialog.setOnDismissListener(ignored -> release());
        dialog.show();

        if (mapView == null) {
            showError(context.getString(R.string.ride_history_route_map_error));
            return;
        }
        mapView.onStart();
        setupMapAndLoadRoute();
    }

    private void attachLifecycleOwnerIfPossible() {
        if (mapView == null || !(context instanceof LifecycleOwner)) {
            return;
        }
        ViewTreeLifecycleOwner.set(mapView, (LifecycleOwner) context);
    }

    private void configureBottomSheet() {
        if (dialog == null) {
            return;
        }
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = (int) (screenHeight * 0.90f);
        bottomSheet.setLayoutParams(layoutParams);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setDraggable(true);
        behavior.setSkipCollapsed(false);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void setupMapAndLoadRoute() {
        if (isTokenMissing()) {
            showError(context.getString(R.string.mapbox_token_missing));
            return;
        }

        MapboxOptions.setAccessToken(BuildConfig.MAPBOX_ACCESS_TOKEN);
        hideDefaultMapboxOrnaments();
        showLoading(context.getString(R.string.ride_history_route_map_loading));

        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
            .center(Point.fromLngLat(DEFAULT_LNG, DEFAULT_LAT))
            .zoom(DEFAULT_ZOOM)
            .build());

        mapView.getMapboxMap().loadStyleUri(MAPBOX_STYLE_URI, style -> loadRoute());
    }

    private void hideDefaultMapboxOrnaments() {
        if (mapView == null) {
            return;
        }
        try {
            LogoUtils.getLogo(mapView).setEnabled(false);
        } catch (Exception ignored) {
        }
        try {
            ScaleBarUtils.getScaleBar(mapView).setEnabled(false);
        } catch (Exception ignored) {
        }
        try {
            AttributionUtils.getAttribution(mapView).setEnabled(false);
        } catch (Exception ignored) {
        }
    }

    private void loadRoute() {
        if (dismissed) {
            return;
        }
        String originAddress = trimToEmpty(ride.getOrigin());
        String destinationAddress = trimToEmpty(ride.getDestination());
        if (originAddress.isEmpty() || destinationAddress.isEmpty()) {
            showError(context.getString(R.string.ride_history_route_map_missing_addresses));
            return;
        }

        geocodeAddress(originAddress, new GeocodeCallback() {
            @Override
            public void onSuccess(@NonNull GeocodedLocation origin) {
                geocodeAddress(destinationAddress, new GeocodeCallback() {
                    @Override
                    public void onSuccess(@NonNull GeocodedLocation destination) {
                        requestRoute(origin, destination);
                    }

                    @Override
                    public void onFailure() {
                        showError(context.getString(R.string.ride_history_route_map_error));
                    }
                });
            }

            @Override
            public void onFailure() {
                showError(context.getString(R.string.ride_history_route_map_error));
            }
        });
    }

    private void geocodeAddress(String address, GeocodeCallback callback) {
        if (geocodingService == null) {
            callback.onFailure();
            return;
        }
        geocodingService.geocodeAddress(Uri.encode(address), 1, BuildConfig.MAPBOX_ACCESS_TOKEN)
            .enqueue(new Callback<MapboxGeocodingResponse>() {
                @Override
                public void onResponse(
                    @NonNull Call<MapboxGeocodingResponse> call,
                    @NonNull Response<MapboxGeocodingResponse> response
                ) {
                    if (dismissed || !response.isSuccessful() || response.body() == null) {
                        callback.onFailure();
                        return;
                    }
                    MapboxGeocodingResponse body = response.body();
                    if (body.features == null || body.features.isEmpty()) {
                        callback.onFailure();
                        return;
                    }
                    MapboxGeocodingResponse.Feature firstFeature = body.features.get(0);
                    if (firstFeature == null || firstFeature.center == null || firstFeature.center.size() < 2) {
                        callback.onFailure();
                        return;
                    }
                    Double lng = firstFeature.center.get(0);
                    Double lat = firstFeature.center.get(1);
                    if (lng == null || lat == null) {
                        callback.onFailure();
                        return;
                    }
                    String normalized = isBlank(firstFeature.place_name) ? address : firstFeature.place_name;
                    callback.onSuccess(new GeocodedLocation(lat, lng, normalized));
                }

                @Override
                public void onFailure(@NonNull Call<MapboxGeocodingResponse> call, @NonNull Throwable t) {
                    callback.onFailure();
                }
            });
    }

    private void requestRoute(GeocodedLocation origin, GeocodedLocation destination) {
        int requestToken = ++routeRequestToken;
        String coordinates = origin.longitude + "," + origin.latitude
            + ";" + destination.longitude + "," + destination.latitude;
        requestDirectionsWithProfile(
            "driving-traffic",
            coordinates,
            requestToken,
            true,
            origin,
            destination
        );
    }

    private void requestDirectionsWithProfile(
        String profile,
        String coordinates,
        int requestToken,
        boolean allowRetryWithDriving,
        GeocodedLocation origin,
        GeocodedLocation destination
    ) {
        if (directionsService == null) {
            showError(context.getString(R.string.ride_history_route_map_error));
            return;
        }
        directionsService.getDirections(
            profile,
            coordinates,
            "geojson",
            "full",
            BuildConfig.MAPBOX_ACCESS_TOKEN
        ).enqueue(new Callback<MapboxDirectionsResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<MapboxDirectionsResponse> call,
                @NonNull Response<MapboxDirectionsResponse> response
            ) {
                if (dismissed || requestToken != routeRequestToken) {
                    return;
                }
                List<List<Double>> coordinatesList = extractRouteCoordinates(response);
                if (coordinatesList != null && coordinatesList.size() >= 2) {
                    drawRoute(coordinatesList, origin, destination);
                    return;
                }
                if (allowRetryWithDriving) {
                    requestDirectionsWithProfile(
                        "driving",
                        coordinates,
                        requestToken,
                        false,
                        origin,
                        destination
                    );
                } else {
                    showError(context.getString(R.string.ride_history_route_map_error));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxDirectionsResponse> call, @NonNull Throwable t) {
                if (dismissed || requestToken != routeRequestToken) {
                    return;
                }
                if (allowRetryWithDriving) {
                    requestDirectionsWithProfile(
                        "driving",
                        coordinates,
                        requestToken,
                        false,
                        origin,
                        destination
                    );
                } else {
                    showError(context.getString(R.string.ride_history_route_map_error));
                }
            }
        });
    }

    @Nullable
    private List<List<Double>> extractRouteCoordinates(Response<MapboxDirectionsResponse> response) {
        if (response == null || !response.isSuccessful() || response.body() == null) {
            return null;
        }
        MapboxDirectionsResponse body = response.body();
        if (body.routes == null || body.routes.isEmpty() || body.routes.get(0) == null) {
            return null;
        }
        MapboxDirectionsResponse.Route route = body.routes.get(0);
        if (route.geometry == null || route.geometry.coordinates == null) {
            return null;
        }
        return route.geometry.coordinates;
    }

    private void drawRoute(
        @NonNull List<List<Double>> routeCoordinates,
        @NonNull GeocodedLocation origin,
        @NonNull GeocodedLocation destination
    ) {
        if (dismissed || mapView == null) {
            return;
        }
        ensureAnnotationManagers();

        List<Point> routePoints = buildRenderableRoutePoints(routeCoordinates, origin);
        if (routePoints.size() < 2 || polylineAnnotationManager == null || pointAnnotationManager == null) {
            showError(context.getString(R.string.ride_history_route_map_error));
            return;
        }

        polylineAnnotationManager.deleteAll();
        pointAnnotationManager.deleteAll();

        polylineAnnotationManager.create(new PolylineAnnotationOptions()
            .withPoints(routePoints)
            .withLineColor("#5B4CDB")
            .withLineWidth(5.2));

        Point startPoint = Point.fromLngLat(origin.longitude, origin.latitude);
        Point endPoint = Point.fromLngLat(destination.longitude, destination.latitude);
        addRouteLabel(startPoint, compactAddressLabel(origin.address), R.color.primary);
        addRouteLabel(endPoint, compactAddressLabel(destination.address), R.color.accent);

        centerMapOnRoute(routePoints);
        showMap();
    }

    private void ensureAnnotationManagers() {
        if (mapView == null) {
            return;
        }
        AnnotationPlugin annotationPlugin = AnnotationsUtils.getAnnotations(mapView);
        if (polylineAnnotationManager == null) {
            polylineAnnotationManager = PolylineAnnotationManagerKt
                .createPolylineAnnotationManager(annotationPlugin, null);
        }
        if (pointAnnotationManager == null) {
            pointAnnotationManager = PointAnnotationManagerKt
                .createPointAnnotationManager(annotationPlugin, null);
        }
    }

    private List<Point> buildRenderableRoutePoints(
        @NonNull List<List<Double>> coordinates,
        @NonNull GeocodedLocation origin
    ) {
        List<Point> points = new ArrayList<>();
        boolean swapOrder = shouldSwapCoordinateOrder(coordinates, origin);
        for (List<Double> pair : coordinates) {
            if (pair == null || pair.size() < 2 || pair.get(0) == null || pair.get(1) == null) {
                continue;
            }
            double lng = swapOrder ? pair.get(1) : pair.get(0);
            double lat = swapOrder ? pair.get(0) : pair.get(1);
            if (!isLatLngInBounds(lat, lng)) {
                continue;
            }
            points.add(Point.fromLngLat(lng, lat));
        }
        return points;
    }

    private boolean shouldSwapCoordinateOrder(
        @NonNull List<List<Double>> coordinates,
        @NonNull GeocodedLocation origin
    ) {
        for (List<Double> pair : coordinates) {
            if (pair == null || pair.size() < 2 || pair.get(0) == null || pair.get(1) == null) {
                continue;
            }
            double firstLng = pair.get(0);
            double firstLat = pair.get(1);
            double swappedLng = pair.get(1);
            double swappedLat = pair.get(0);
            double normalDistance = square(firstLng - origin.longitude) + square(firstLat - origin.latitude);
            double swappedDistance = square(swappedLng - origin.longitude) + square(swappedLat - origin.latitude);
            return swappedDistance + 0.000001 < normalDistance;
        }
        return false;
    }

    private void addRouteLabel(Point point, String label, int accentColorRes) {
        if (pointAnnotationManager == null) {
            return;
        }
        Bitmap icon = createRouteEndpointLabelBitmap(
            label,
            ContextCompat.getColor(context, accentColorRes)
        );
        if (icon == null) {
            return;
        }
        pointAnnotationManager.create(new PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(icon)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withIconOffset(ROUTE_LABEL_ICON_OFFSET));
    }

    private Bitmap createRouteEndpointLabelBitmap(String label, int accentColor) {
        float density = context.getResources().getDisplayMetrics().density;
        float bubbleHeight = 30f * density;
        float tailHeight = 8f * density;
        float radius = 11f * density;
        float strokeWidth = 1f * density;
        float horizontalPadding = 12f * density;
        float dotRadius = 3.5f * density;
        float textGap = 7f * density;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(context, R.color.text_dark));
        textPaint.setTextSize(12f * density);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float textWidth = textPaint.measureText(label);

        int width = Math.round(Math.max(96f * density, horizontalPadding * 2f + dotRadius * 2f + textGap + textWidth));
        int height = Math.round(bubbleHeight + tailHeight);
        float centerX = width / 2f;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF bubbleRect = new RectF(0f, 0f, width, bubbleHeight);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(35, 31, 41, 55));
        canvas.drawRoundRect(
            bubbleRect.left,
            bubbleRect.top + (1.5f * density),
            bubbleRect.right,
            bubbleRect.bottom + (1.5f * density),
            radius,
            radius,
            shadowPaint
        );

        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setColor(ContextCompat.getColor(context, R.color.white));
        canvas.drawRoundRect(bubbleRect, radius, radius, bubblePaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(Color.argb(45, 31, 31, 31));
        canvas.drawRoundRect(bubbleRect, radius, radius, strokePaint);

        Path tail = new Path();
        tail.moveTo(centerX - (6f * density), bubbleHeight - strokeWidth);
        tail.lineTo(centerX + (6f * density), bubbleHeight - strokeWidth);
        tail.lineTo(centerX, bubbleHeight + tailHeight);
        tail.close();
        canvas.drawPath(tail, bubblePaint);
        canvas.drawPath(tail, strokePaint);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(accentColor);
        float dotCenterX = horizontalPadding + dotRadius;
        float dotCenterY = bubbleHeight / 2f;
        canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textX = dotCenterX + dotRadius + textGap;
        float textY = bubbleHeight / 2f - ((fontMetrics.ascent + fontMetrics.descent) / 2f);
        canvas.drawText(label, textX, textY, textPaint);
        return bitmap;
    }

    private String compactAddressLabel(String address) {
        if (isBlank(address)) {
            return context.getString(R.string.ride_history_none);
        }
        String cleaned = address.trim();
        int commaIndex = cleaned.indexOf(',');
        if (commaIndex > 0) {
            cleaned = cleaned.substring(0, commaIndex).trim();
        }
        if (cleaned.length() > 26) {
            return cleaned.substring(0, 25) + "...";
        }
        return cleaned;
    }

    private void centerMapOnRoute(List<Point> points) {
        if (mapView == null || points == null || points.isEmpty()) {
            return;
        }
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;
        for (Point point : points) {
            if (point == null) {
                continue;
            }
            minLat = Math.min(minLat, point.latitude());
            maxLat = Math.max(maxLat, point.latitude());
            minLng = Math.min(minLng, point.longitude());
            maxLng = Math.max(maxLng, point.longitude());
        }
        if (minLat == Double.MAX_VALUE || minLng == Double.MAX_VALUE) {
            return;
        }
        double centerLat = (minLat + maxLat) / 2.0;
        double centerLng = (minLng + maxLng) / 2.0;
        double span = Math.max(maxLat - minLat, maxLng - minLng);
        animateCameraTo(centerLng, centerLat, resolveZoom(span));
    }

    private void animateCameraTo(double targetLng, double targetLat, double targetZoom) {
        if (mapView == null) {
            return;
        }
        if (cameraAnimator != null) {
            cameraAnimator.cancel();
        }
        double startLng = mapView.getMapboxMap().getCameraState().getCenter().longitude();
        double startLat = mapView.getMapboxMap().getCameraState().getCenter().latitude();
        double startZoom = mapView.getMapboxMap().getCameraState().getZoom();

        cameraAnimator = ValueAnimator.ofFloat(0f, 1f);
        cameraAnimator.setDuration(CAMERA_ANIMATION_MS);
        cameraAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        cameraAnimator.addUpdateListener(animation -> {
            if (mapView == null || dismissed) {
                return;
            }
            float t = (float) animation.getAnimatedValue();
            double lng = startLng + (targetLng - startLng) * t;
            double lat = startLat + (targetLat - startLat) * t;
            double zoom = startZoom + (targetZoom - startZoom) * t;
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(zoom)
                .build());
        });
        cameraAnimator.start();
    }

    private double resolveZoom(double span) {
        if (span <= 0.01) {
            return 14.8;
        }
        if (span <= 0.03) {
            return 13.8;
        }
        if (span <= 0.08) {
            return 12.7;
        }
        if (span <= 0.2) {
            return 11.6;
        }
        if (span <= 0.5) {
            return 10.8;
        }
        return 10.1;
    }

    private void showLoading(String message) {
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(ContextCompat.getColor(context, R.color.text_medium));
        }
    }

    private void showMap() {
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(ContextCompat.getColor(context, R.color.danger));
        }
    }

    private void release() {
        dismissed = true;
        routeRequestToken++;
        if (cameraAnimator != null) {
            cameraAnimator.cancel();
            cameraAnimator = null;
        }
        if (pointAnnotationManager != null) {
            pointAnnotationManager.deleteAll();
        }
        if (polylineAnnotationManager != null) {
            polylineAnnotationManager.deleteAll();
        }
        if (mapView != null) {
            mapView.onStop();
            mapView.onDestroy();
        }
        pointAnnotationManager = null;
        polylineAnnotationManager = null;
        mapView = null;
        overlay = null;
        statusText = null;
        progressBar = null;
        dialog = null;
    }

    private boolean isTokenMissing() {
        String token = BuildConfig.MAPBOX_ACCESS_TOKEN;
        return token == null || token.trim().isEmpty() || "MAPBOX_API_KEY".equals(token);
    }

    private boolean isLatLngInBounds(double lat, double lng) {
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static double square(double value) {
        return value * value;
    }

    private interface GeocodeCallback {
        void onSuccess(@NonNull GeocodedLocation location);
        void onFailure();
    }

    private static final class GeocodedLocation {
        final double latitude;
        final double longitude;
        final String address;

        GeocodedLocation(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }
    }
}
