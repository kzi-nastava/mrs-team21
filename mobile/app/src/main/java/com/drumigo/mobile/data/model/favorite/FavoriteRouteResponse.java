package com.drumigo.mobile.data.model.favorite;

import java.util.List;

/**
 * Favorite route for order-ride picker. Matches backend FavoriteRouteResponse.
 */
public class FavoriteRouteResponse {
    public Long id;
    public Long passengerId;
    public Long vehicleTypeId;
    public String vehicleTypeName;
    public Boolean babyTransport;
    public Boolean petTransport;
    public List<FavoriteRouteWaypointResponse> waypoints;
    public String createdAt;
}
