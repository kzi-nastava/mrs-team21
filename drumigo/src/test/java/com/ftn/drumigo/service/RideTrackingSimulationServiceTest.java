package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.dto.VehicleLocationUpdateRequest;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.repository.RideWaypointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideTrackingSimulationServiceTest {

    @Mock
    private RideService rideService;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private RideWaypointRepository rideWaypointRepository;
    @Mock
    private MapService mapService;

    @InjectMocks
    private RideTrackingSimulationService simulationService;

    @Test
    void startSimulation_whenRideIsActive_usesMapboxRouteAndAdvancesAlongRoute() {
        Ride ride = activeRide(10L, 55L, 900);
        List<RideWaypoint> waypoints = List.of(
            waypoint(ride, 0, 45.25100, 19.84500),
            waypoint(ride, 1, 45.25500, 19.85000),
            waypoint(ride, 2, 45.26000, 19.86000)
        );

        when(rideService.getById(10L)).thenReturn(ride);
        when(rideWaypointRepository.findByRideWithLocationOrderByWaypointOrderAsc(ride)).thenReturn(waypoints);
        when(mapService.getRouteCoordinatesForOrderedWaypoints(anyList())).thenReturn(List.of(
            List.of(19.84500, 45.25100),
            List.of(19.84800, 45.25300),
            List.of(19.85200, 45.25600),
            List.of(19.86000, 45.26000)
        ));

        simulationService.startSimulation(10L);
        simulationService.tick();

        verify(mapService).getRouteCoordinatesForOrderedWaypoints(anyList());
        verify(vehicleService, atLeast(2))
            .updateCurrentDriverLocation(eq(55L), any(VehicleLocationUpdateRequest.class));
    }

    @Test
    void tick_whenRouteIsCompleted_stopsSimulationAtDestination() {
        Ride ride = activeRide(11L, 66L, 60);
        List<RideWaypoint> waypoints = List.of(
            waypoint(ride, 0, 45.25100, 19.84500),
            waypoint(ride, 1, 45.25130, 19.84530)
        );

        when(rideService.getById(11L)).thenReturn(ride);
        when(rideWaypointRepository.findByRideWithLocationOrderByWaypointOrderAsc(ride)).thenReturn(waypoints);
        when(mapService.getRouteCoordinatesForOrderedWaypoints(anyList())).thenReturn(List.of(
            List.of(19.84500, 45.25100),
            List.of(19.84530, 45.25130)
        ));

        simulationService.startSimulation(11L);
        for (int i = 0; i < 40; i++) {
            simulationService.tick();
        }

        ArgumentCaptor<VehicleLocationUpdateRequest> requestCaptor =
            ArgumentCaptor.forClass(VehicleLocationUpdateRequest.class);
        verify(vehicleService, atLeast(2))
            .updateCurrentDriverLocation(eq(66L), requestCaptor.capture());

        List<VehicleLocationUpdateRequest> updates = requestCaptor.getAllValues();
        VehicleLocationUpdateRequest finalUpdate = updates.get(updates.size() - 1);
        assertTrue(Math.abs(finalUpdate.lat().doubleValue() - 45.25130) < 0.00001);
        assertTrue(Math.abs(finalUpdate.lng().doubleValue() - 19.84530) < 0.00001);
    }

    @Test
    void startSimulation_whenRideIsNotActive_throwsBadRequest() {
        Ride ride = activeRide(12L, 77L, 600);
        ride.setStatus(RideStatus.ACCEPTED);
        when(rideService.getById(12L)).thenReturn(ride);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> simulationService.startSimulation(12L));

        assertEquals("Ride must be ACTIVE to start tracking demo. Current status: ACCEPTED", ex.getMessage());
        verifyNoInteractions(rideWaypointRepository, mapService, vehicleService);
    }

    private Ride activeRide(Long rideId, Long driverId, Integer estimatedDurationSec) {
        Ride ride = new Ride();
        ride.setId(rideId);
        ride.setStatus(RideStatus.ACTIVE);
        ride.setEstimatedDurationSec(estimatedDurationSec);
        Driver driver = new Driver();
        driver.setId(driverId);
        ride.setDriver(driver);
        ride.setVehicle(new Vehicle());
        return ride;
    }

    private RideWaypoint waypoint(Ride ride, int order, double lat, double lng) {
        Location location = new Location();
        location.setId((long) order + 1);
        location.setAddress("Waypoint " + order);
        location.setLat(BigDecimal.valueOf(lat));
        location.setLng(BigDecimal.valueOf(lng));

        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setLocation(location);
        waypoint.setWaypointOrder(order);
        return waypoint;
    }
}
