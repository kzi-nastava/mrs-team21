package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:ride_waypoint_repo_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.test.database.replace=NONE",
    "maintenance.basic.username=admin",
    "maintenance.basic.password=admin",
    "jwt.secret=testSecretKeyThatIsAtLeast32CharactersLongForHS256",
    "jwt.expiration=3600000"
})
class RideWaypointRepositoryTest {

    @Autowired
    private RideWaypointRepository rideWaypointRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void shouldFindWaypointsOrderedAscendingByWaypointOrder() {
        Ride ride = createRide();
        RideWaypoint wp2 = createWaypoint(ride, "Destination", "45.26000000", "19.88000000", 2);
        RideWaypoint wp0 = createWaypoint(ride, "Pickup", "45.25100000", "19.84500000", 0);
        RideWaypoint wp1 = createWaypoint(ride, "Midpoint", "45.25500000", "19.86000000", 1);
        rideWaypointRepository.saveAll(List.of(wp2, wp0, wp1));

        List<RideWaypoint> ordered = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);

        assertEquals(3, ordered.size());
        assertEquals(0, ordered.get(0).getWaypointOrder());
        assertEquals(1, ordered.get(1).getWaypointOrder());
        assertEquals(2, ordered.get(2).getWaypointOrder());
    }

    @Test
    void shouldFindLastWaypointByHighestOrder() {
        Ride ride = createRide();
        rideWaypointRepository.save(createWaypoint(ride, "Pickup", "45.25100000", "19.84500000", 0));
        rideWaypointRepository.save(createWaypoint(ride, "Midpoint", "45.25500000", "19.86000000", 1));
        rideWaypointRepository.save(createWaypoint(ride, "Destination", "45.26000000", "19.88000000", 2));

        Optional<RideWaypoint> last = rideWaypointRepository.findFirstByRideOrderByWaypointOrderDesc(ride);

        assertTrue(last.isPresent());
        assertEquals(2, last.get().getWaypointOrder());
        assertEquals("Destination", last.get().getLocation().getAddress());
    }

    @Test
    void shouldDeleteWaypointsWithOrderGreaterThanThreshold() {
        Ride ride = createRide();
        rideWaypointRepository.save(createWaypoint(ride, "Pickup", "45.25100000", "19.84500000", 0));
        rideWaypointRepository.save(createWaypoint(ride, "Midpoint", "45.25500000", "19.86000000", 1));
        rideWaypointRepository.save(createWaypoint(ride, "Destination", "45.26000000", "19.88000000", 2));

        rideWaypointRepository.deleteByRideAndWaypointOrderGreaterThan(ride, 0);
        List<RideWaypoint> remaining = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);

        assertEquals(1, remaining.size());
        assertEquals(0, remaining.get(0).getWaypointOrder());
        assertEquals("Pickup", remaining.get(0).getLocation().getAddress());
    }

    private Ride createRide() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.ACTIVE);
        return rideRepository.save(ride);
    }

    private RideWaypoint createWaypoint(Ride ride, String address, String lat, String lng, int order) {
        Location location = new Location();
        location.setAddress(address);
        location.setLat(new BigDecimal(lat));
        location.setLng(new BigDecimal(lng));
        location = locationRepository.save(location);

        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setLocation(location);
        waypoint.setWaypointOrder(order);
        return waypoint;
    }
}
