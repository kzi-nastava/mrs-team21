export interface RoutingPoint {
  lat: number;
  lng: number;
}

export interface OrderedWaypoint extends RoutingPoint {
  order: number;
}

export interface RemainingWaypointRouteResult {
  routeRequestPoints: RoutingPoint[];
  lastPassedWaypointOrder: number;
}

const EARTH_RADIUS_METERS = 6_371_000;
const SAME_POINT_TOLERANCE = 1e-8;

export const DEFAULT_WAYPOINT_REACHED_RADIUS_METERS = 70;

export function buildRouteRequestThroughRemainingWaypoints(
  currentLocation: RoutingPoint,
  orderedWaypoints: OrderedWaypoint[],
  currentLastPassedWaypointOrder: number,
  waypointReachedRadiusMeters: number = DEFAULT_WAYPOINT_REACHED_RADIUS_METERS,
): RemainingWaypointRouteResult {
  const sortedWaypoints = [...(orderedWaypoints ?? [])].sort((a, b) => a.order - b.order);
  if (sortedWaypoints.length === 0) {
    return {
      routeRequestPoints: [currentLocation],
      lastPassedWaypointOrder: currentLastPassedWaypointOrder,
    };
  }

  const nonDestinationWaypoints = sortedWaypoints.slice(0, -1);
  const reachedWaypointOrder = nonDestinationWaypoints
    .filter((waypoint) => distanceMeters(currentLocation, waypoint) <= waypointReachedRadiusMeters)
    .reduce(
      (maxOrder, waypoint) => Math.max(maxOrder, waypoint.order),
      Number.NEGATIVE_INFINITY,
    );

  const lastPassedWaypointOrder = Math.max(
    currentLastPassedWaypointOrder,
    reachedWaypointOrder,
  );

  return {
    routeRequestPoints: buildRouteRequestPointsFromLastPassed(
      currentLocation,
      sortedWaypoints,
      lastPassedWaypointOrder,
    ),
    lastPassedWaypointOrder,
  };
}

export function buildRouteRequestPointsFromLastPassed(
  origin: RoutingPoint,
  orderedWaypoints: OrderedWaypoint[],
  lastPassedWaypointOrder: number,
): RoutingPoint[] {
  const sortedWaypoints = [...(orderedWaypoints ?? [])].sort((a, b) => a.order - b.order);
  if (sortedWaypoints.length === 0) {
    return [origin];
  }

  const remainingWaypoints = sortedWaypoints.filter(
    (waypoint) => waypoint.order > lastPassedWaypointOrder,
  );
  const destination = sortedWaypoints[sortedWaypoints.length - 1];
  const routeRequestPoints = deduplicateConsecutivePoints([
    origin,
    ...remainingWaypoints.map((waypoint) => ({ lat: waypoint.lat, lng: waypoint.lng })),
  ]);

  if (routeRequestPoints.length < 2) {
    routeRequestPoints.push({ lat: destination.lat, lng: destination.lng });
  }

  return deduplicateConsecutivePoints(routeRequestPoints);
}

export function distanceMeters(a: RoutingPoint, b: RoutingPoint): number {
  const lat1 = toRadians(a.lat);
  const lat2 = toRadians(b.lat);
  const dLat = lat2 - lat1;
  const dLng = toRadians(b.lng - a.lng);
  const sinLat = Math.sin(dLat / 2);
  const sinLng = Math.sin(dLng / 2);
  const haversine =
    sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng;
  return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
}

function deduplicateConsecutivePoints(points: RoutingPoint[]): RoutingPoint[] {
  if (points.length < 2) {
    return points;
  }
  const result: RoutingPoint[] = [points[0]];
  for (let i = 1; i < points.length; i++) {
    const previous = result[result.length - 1];
    const current = points[i];
    if (!isSamePoint(previous, current)) {
      result.push(current);
    }
  }
  return result;
}

function isSamePoint(a: RoutingPoint, b: RoutingPoint): boolean {
  return (
    Math.abs(a.lat - b.lat) <= SAME_POINT_TOLERANCE &&
    Math.abs(a.lng - b.lng) <= SAME_POINT_TOLERANCE
  );
}

function toRadians(value: number): number {
  return (value * Math.PI) / 180;
}
