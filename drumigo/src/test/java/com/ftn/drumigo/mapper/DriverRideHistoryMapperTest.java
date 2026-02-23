package com.ftn.drumigo.mapper;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.history.response.DriverRideHistoryItemResponse;
import com.ftn.drumigo.repository.PanicEventRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import com.ftn.drumigo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverRideHistoryMapperTest {

    @Mock
    private RidePassengerRepository ridePassengerRepository;
    @Mock
    private RideWaypointRepository rideWaypointRepository;
    @Mock
    private PanicEventRepository panicEventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DriverRideHistoryMapper mapper;

    @Test
    void toResponse_whenWaypointOrdersStartAtOne_mapsStartAndEndLocations() {
        Ride ride = new Ride();
        ride.setId(900L);
        ride.setStatus(RideStatus.FINISHED);

        RideWaypoint pickup = waypoint(ride, location("Pickup address", 45.25, 19.84), 1);
        RideWaypoint destination = waypoint(ride, location("Destination address", 45.28, 19.87), 2);

        when(rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride))
            .thenReturn(List.of(pickup, destination));
        when(ridePassengerRepository.findByRide(ride)).thenReturn(List.of());
        when(panicEventRepository.findByRide(ride)).thenReturn(List.of());

        DriverRideHistoryItemResponse response = mapper.toResponse(ride);

        assertNotNull(response.startLocation());
        assertNotNull(response.endLocation());
        assertEquals("Pickup address", response.startLocation().address());
        assertEquals("Destination address", response.endLocation().address());
    }

    private static Location location(String address, double lat, double lng) {
        Location location = new Location();
        location.setAddress(address);
        location.setLat(BigDecimal.valueOf(lat));
        location.setLng(BigDecimal.valueOf(lng));
        return location;
    }

    private static RideWaypoint waypoint(Ride ride, Location location, int order) {
        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setLocation(location);
        waypoint.setWaypointOrder(order);
        return waypoint;
    }
}
