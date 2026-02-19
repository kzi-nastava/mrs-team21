import {
  buildRouteRequestThroughRemainingWaypoints,
  DEFAULT_WAYPOINT_REACHED_RADIUS_METERS,
} from './remaining-waypoints.util';

describe('remaining-waypoints util', () => {
  it('builds route points as current + remaining waypoints + destination', () => {
    const result = buildRouteRequestThroughRemainingWaypoints(
      { lat: 45.251, lng: 19.845 },
      [
        { lat: 45.251, lng: 19.845, order: 0 },
        { lat: 45.255, lng: 19.851, order: 1 },
        { lat: 45.261, lng: 19.861, order: 2 },
      ],
      Number.NEGATIVE_INFINITY,
      DEFAULT_WAYPOINT_REACHED_RADIUS_METERS,
    );

    expect(result.lastPassedWaypointOrder).toBe(0);
    expect(result.routeRequestPoints).toEqual([
      { lat: 45.251, lng: 19.845 },
      { lat: 45.255, lng: 19.851 },
      { lat: 45.261, lng: 19.861 },
    ]);
  });

  it('keeps passed waypoint progress monotonic and excludes already passed intermediates', () => {
    const result = buildRouteRequestThroughRemainingWaypoints(
      { lat: 45.255, lng: 19.851 },
      [
        { lat: 45.251, lng: 19.845, order: 0 },
        { lat: 45.255, lng: 19.851, order: 1 },
        { lat: 45.261, lng: 19.861, order: 2 },
      ],
      0,
      DEFAULT_WAYPOINT_REACHED_RADIUS_METERS,
    );

    expect(result.lastPassedWaypointOrder).toBe(1);
    expect(result.routeRequestPoints).toEqual([
      { lat: 45.255, lng: 19.851 },
      { lat: 45.261, lng: 19.861 },
    ]);
  });
});
