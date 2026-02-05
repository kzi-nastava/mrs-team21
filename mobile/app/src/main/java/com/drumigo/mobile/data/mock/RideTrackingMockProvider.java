package com.drumigo.mobile.data.mock;

import android.os.Handler;
import android.os.Looper;

import com.drumigo.mobile.data.model.ride.ActiveRide;
import com.drumigo.mobile.data.model.ride.LocationUpdate;
import com.drumigo.mobile.data.model.ride.RoutePoint;

import java.util.ArrayList;
import java.util.List;

public class RideTrackingMockProvider {

    private static final long UPDATE_INTERVAL_MS = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private ActiveRide mockRide;
    private int currentStep = 0;
    private double progress = 0.0;
    private RoutePoint previousLocation;

    public RideTrackingMockProvider() {
        mockRide = buildMockRide();
    }

    public ActiveRide getActiveRide(long rideId) {
        ActiveRide ride = buildMockRide();
        ride.id = rideId;
        ride.estimatedArrivalTimeSec = calculateETA(ride.currentLocation, ride.destinationLocation);
        return ride;
    }

    public void startLocationUpdates(long rideId, LocationUpdateListener listener) {
        stopLocationUpdates();
        ActiveRide ride = getActiveRide(rideId);
        currentStep = 0;
        progress = 0.0;
        previousLocation = null;

        updateRunnable = () -> {
            LocationUpdate update = nextUpdate(ride);
            if (listener != null && update != null) {
                listener.onLocationUpdate(update);
            }
            handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
        };
        handler.post(updateRunnable);
    }

    public void stopLocationUpdates() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private LocationUpdate nextUpdate(ActiveRide ride) {
        List<RoutePoint> route = ride.route == null ? new ArrayList<>() : ride.route;
        if (route.size() < 2) {
            return null;
        }

        int totalSteps = route.size() - 1;
        if (currentStep >= totalSteps) {
            RoutePoint destination = ride.destinationLocation;
            float bearing = previousLocation == null
                ? 0f
                : (float) calculateBearing(previousLocation, destination);
            return new LocationUpdate(
                destination.lat,
                destination.lng,
                System.currentTimeMillis(),
                0,
                bearing
            );
        }

        RoutePoint currentWaypoint = route.get(currentStep);
        RoutePoint nextWaypoint = route.get(currentStep + 1);
        double stepProgress = progress % 1.0;
        double lat = currentWaypoint.lat + (nextWaypoint.lat - currentWaypoint.lat) * stepProgress;
        double lng = currentWaypoint.lng + (nextWaypoint.lng - currentWaypoint.lng) * stepProgress;

        progress += 0.15;
        if (progress >= 1.0) {
            currentStep++;
            progress = 0.0;
        }

        RoutePoint currentLocation = new RoutePoint(lat, lng, currentStep);
        int eta = calculateETA(currentLocation, ride.destinationLocation);

        float bearing;
        if (previousLocation != null) {
            bearing = (float) calculateBearing(previousLocation, currentLocation);
        } else {
            RoutePoint lookAhead = route.get(Math.min(currentStep + 1, totalSteps));
            bearing = (float) calculateBearing(currentLocation, lookAhead);
        }

        previousLocation = currentLocation;

        return new LocationUpdate(
            lat,
            lng,
            System.currentTimeMillis(),
            eta,
            bearing
        );
    }

    private ActiveRide buildMockRide() {
        ActiveRide ride = new ActiveRide();
        ride.id = 1L;
        ride.status = "ACTIVE";
        ride.driverName = "Marko";
        ride.driverSurname = "Petrovic";
        ride.vehicleModel = "Toyota Corolla";
        ride.vehicleLicensePlate = "NS-123-AB";
        ride.startAddress = "Trg Slobode, Novi Sad";
        ride.destinationAddress = "Petrovaradin Fortress, Novi Sad";

        ride.startLocation = new RoutePoint(45.2671, 19.8335, 0);
        ride.destinationLocation = new RoutePoint(45.2517, 19.8369, 5);
        ride.currentLocation = new RoutePoint(45.2671, 19.8335, 0);

        List<RoutePoint> route = new ArrayList<>();
        route.add(new RoutePoint(45.2671, 19.8335, 0));
        route.add(new RoutePoint(45.2650, 19.8345, 1));
        route.add(new RoutePoint(45.2620, 19.8355, 2));
        route.add(new RoutePoint(45.2590, 19.8360, 3));
        route.add(new RoutePoint(45.2560, 19.8365, 4));
        route.add(new RoutePoint(45.2517, 19.8369, 5));
        ride.route = route;
        ride.estimatedArrivalTimeSec = calculateETA(ride.currentLocation, ride.destinationLocation);
        return ride;
    }

    private int calculateETA(RoutePoint currentLocation, RoutePoint destination) {
        double distanceMeters = calculateDistance(currentLocation, destination);
        double averageSpeedKmh = 50.0;
        double averageSpeedMs = averageSpeedKmh / 3.6;
        double timeSeconds = distanceMeters / averageSpeedMs;
        return Math.max(0, (int) Math.round(timeSeconds));
    }

    private double calculateDistance(RoutePoint point1, RoutePoint point2) {
        double R = 6371000.0;
        double dLat = toRad(point2.lat - point1.lat);
        double dLng = toRad(point2.lng - point1.lng);
        double lat1 = toRad(point1.lat);
        double lat2 = toRad(point2.lat);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(lat1) * Math.cos(lat2)
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculateBearing(RoutePoint from, RoutePoint to) {
        double dLng = toRad(to.lng - from.lng);
        double lat1 = toRad(from.lat);
        double lat2 = toRad(to.lat);

        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
            - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        double bearing = Math.atan2(y, x);
        bearing = toDeg(bearing);
        return (bearing + 360.0) % 360.0;
    }

    private double toRad(double degrees) {
        return degrees * Math.PI / 180.0;
    }

    private double toDeg(double radians) {
        return radians * 180.0 / Math.PI;
    }

    public interface LocationUpdateListener {
        void onLocationUpdate(LocationUpdate update);
    }
}
