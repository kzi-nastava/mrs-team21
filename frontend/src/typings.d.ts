declare module '@mapbox/mapbox-sdk/services/geocoding' {
  interface ForwardGeocodeParams {
    query: string;
    limit?: number;
  }

  interface GeocodingFeature {
    center: [number, number];
    place_name: string;
  }

  interface GeocodingResponse {
    body?: { features?: GeocodingFeature[] };
  }

  function mbxGeocoding(opts?: { accessToken?: string }): {
    forwardGeocode(params: ForwardGeocodeParams): { send(): Promise<GeocodingResponse> };
  };

  export default mbxGeocoding;
}

declare module '@mapbox/mapbox-sdk' {
  const content: any;
  export default content;
}
