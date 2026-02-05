package com.drumigo.mobile.data.model.mapbox;

import java.util.List;

public class MapboxDirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public Geometry geometry;
    }

    public static class Geometry {
        public String type;
        public List<List<Double>> coordinates;
    }
}
