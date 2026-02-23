package com.drumigo.mobile.data.model.favorite;

/**
 * Waypoint of a favorite route. Matches backend FavoriteRouteResponse.WaypointResponse.
 * lat/lng may come as numbers from JSON; use Double for Gson.
 */
public class FavoriteRouteWaypointResponse {
    public Long id;
    public Long locationId;
    public String address;
    public Double lat;
    public Double lng;
    public Integer order;
}
