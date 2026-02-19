package com.drumigo.mobile.data.model.mapbox;

import java.util.List;

public class MapboxGeocodingResponse {
    public List<Feature> features;

    public static class Feature {
        public String place_name;
        public List<Double> center;
    }
}
