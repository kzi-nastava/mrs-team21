package com.drumigo.mobile.data.model.mapbox;

import java.util.List;

/**
 * Response from Mapbox Search Box API suggest endpoint.
 * https://api.mapbox.com/search/searchbox/v1/suggest
 */
public class MapboxSearchBoxSuggestResponse {

    public List<Suggestion> suggestions;

    public static class Suggestion {
        public String full_address;
        public String name;
        public String address;
        public String place_formatted;
    }
}
