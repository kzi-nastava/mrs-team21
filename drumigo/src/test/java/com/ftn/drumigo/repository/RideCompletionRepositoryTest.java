package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RideCompletionRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private PassengerRepository passengerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private RidePassengerRepository ridePassengerRepository;
    @Autowired
    private RideWaypointRepository rideWaypointRepository;
    @Autowired
    private LocationRepository locationRepository;

    @Test
    void driverRepository_findByActiveDriverTrue_returnsOnlyActiveDrivers() {
        Driver activeDriver = persistDriver("active.driver@mail.com", true);
        Driver inactiveDriver = persistDriver("inactive.driver@mail.com", false);

        List<Driver> result = driverRepository.findByActiveDriverTrue();
        Set<String> emails = result.stream().map(Driver::getEmail).collect(Collectors.toSet());

        assertTrue(emails.contains(activeDriver.getEmail()));
        assertFalse(emails.contains(inactiveDriver.getEmail()));
    }

    @Test
    void driverRepository_findByEmail_returnsMatchingDriver() {
        Driver saved = persistDriver("find.by.email.driver@mail.com", true);

        Driver found = driverRepository.findByEmail("find.by.email.driver@mail.com").orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals("find.by.email.driver@mail.com", found.getEmail());
    }

    @Test
    void userRepository_findByEmailAndExistsByEmail_workAsExpected() {
        Passenger passenger = persistPassenger("user.repo.passenger@mail.com");

        assertTrue(userRepository.findByEmail("user.repo.passenger@mail.com").isPresent());
        assertTrue(userRepository.existsByEmail("user.repo.passenger@mail.com"));
        assertFalse(userRepository.existsByEmail("missing.user@mail.com"));
        assertEquals(passenger.getId(), userRepository.findByEmail("user.repo.passenger@mail.com").orElseThrow().getId());
    }

    @Test
    void ridePassengerRepository_findByRide_returnsOnlyPassengersForRequestedRide() {
        Driver driver = persistDriver("driver.for.ride.passengers@mail.com", true);
        Passenger orderingPassenger = persistPassenger("ordering.passenger@mail.com");
        Ride firstRide = persistRide(driver, orderingPassenger, RideStatus.FINISHED);
        Ride secondRide = persistRide(driver, orderingPassenger, RideStatus.FINISHED);

        ridePassengerRepository.saveAndFlush(new RidePassenger(firstRide, "linked.one@mail.com"));
        ridePassengerRepository.saveAndFlush(new RidePassenger(firstRide, "linked.two@mail.com"));
        ridePassengerRepository.saveAndFlush(new RidePassenger(secondRide, "other.ride@mail.com"));

        List<RidePassenger> result = ridePassengerRepository.findByRide(firstRide);
        Set<String> emails = result.stream().map(RidePassenger::getPassengerEmail).collect(Collectors.toSet());

        assertEquals(2, result.size());
        assertEquals(Set.of("linked.one@mail.com", "linked.two@mail.com"), emails);
    }

    @Test
    void ridePassengerRepository_existsByRideAndPassengerEmail_checksMembership() {
        Driver driver = persistDriver("driver.for.membership@mail.com", true);
        Passenger orderingPassenger = persistPassenger("ordering.membership@mail.com");
        Ride ride = persistRide(driver, orderingPassenger, RideStatus.FINISHED);
        ridePassengerRepository.saveAndFlush(new RidePassenger(ride, "linked.member@mail.com"));

        assertTrue(ridePassengerRepository.existsByRideAndPassengerEmail(ride, "linked.member@mail.com"));
        assertFalse(ridePassengerRepository.existsByRideAndPassengerEmail(ride, "missing.member@mail.com"));
    }

    @Test
    void rideWaypointRepository_findByRideOrderByWaypointOrderAsc_returnsSortedWaypoints() {
        Driver driver = persistDriver("driver.waypoint.sort@mail.com", true);
        Passenger orderingPassenger = persistPassenger("ordering.waypoint.sort@mail.com");
        Ride ride = persistRide(driver, orderingPassenger, RideStatus.FINISHED);

        persistWaypoint(ride, 2, "Destination");
        persistWaypoint(ride, 0, "Pickup");
        persistWaypoint(ride, 1, "Middle stop");

        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);

        assertEquals(List.of(0, 1, 2), waypoints.stream().map(RideWaypoint::getWaypointOrder).toList());
        assertEquals(List.of("Pickup", "Middle stop", "Destination"),
            waypoints.stream().map(wp -> wp.getLocation().getAddress()).toList());
    }

    @Test
    void rideWaypointRepository_findFirstByRideOrderByWaypointOrderDesc_returnsLastWaypoint() {
        Driver driver = persistDriver("driver.waypoint.last@mail.com", true);
        Passenger orderingPassenger = persistPassenger("ordering.waypoint.last@mail.com");
        Ride ride = persistRide(driver, orderingPassenger, RideStatus.FINISHED);

        persistWaypoint(ride, 0, "Pickup");
        persistWaypoint(ride, 1, "Middle stop");
        persistWaypoint(ride, 2, "Destination");

        RideWaypoint last = rideWaypointRepository.findFirstByRideOrderByWaypointOrderDesc(ride).orElseThrow();

        assertEquals(2, last.getWaypointOrder());
        assertEquals("Destination", last.getLocation().getAddress());
    }

    @Test
    void rideWaypointRepository_deleteByRideAndWaypointOrderGreaterThan_removesHigherOrderWaypoints() {
        Driver driver = persistDriver("driver.waypoint.delete@mail.com", true);
        Passenger orderingPassenger = persistPassenger("ordering.waypoint.delete@mail.com");
        Ride ride = persistRide(driver, orderingPassenger, RideStatus.ACTIVE);

        persistWaypoint(ride, 0, "Pickup");
        persistWaypoint(ride, 1, "Middle stop");
        persistWaypoint(ride, 2, "Destination");

        rideWaypointRepository.deleteByRideAndWaypointOrderGreaterThan(ride, 0);
        List<RideWaypoint> remaining = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        remaining.sort(Comparator.comparingInt(RideWaypoint::getWaypointOrder));

        assertEquals(1, remaining.size());
        assertEquals(0, remaining.get(0).getWaypointOrder());
        assertEquals("Pickup", remaining.get(0).getLocation().getAddress());
    }

    private Driver persistDriver(String email, boolean activeDriver) {
        Driver driver = new Driver();
        driver.setName("Driver");
        driver.setSurname("Test");
        driver.setEmail(email);
        driver.setActive(true);
        driver.setRole(UserRole.DRIVER);
        driver.setBlocked(false);
        driver.setActiveDriver(activeDriver);
        driver.setBusy(false);
        return driverRepository.saveAndFlush(driver);
    }

    private Passenger persistPassenger(String email) {
        Passenger passenger = new Passenger();
        passenger.setName("Passenger");
        passenger.setSurname("Test");
        passenger.setEmail(email);
        passenger.setActive(true);
        passenger.setRole(UserRole.PASSENGER);
        passenger.setBlocked(false);
        return passengerRepository.saveAndFlush(passenger);
    }

    private Ride persistRide(Driver driver, Passenger orderingPassenger, RideStatus status) {
        Ride ride = new Ride();
        ride.setStatus(status);
        ride.setRequestedAt(Instant.now());
        ride.setDriver(driver);
        ride.setOrderingPassenger(orderingPassenger);
        ride.setBabyTransport(false);
        ride.setPetTransport(false);
        return rideRepository.saveAndFlush(ride);
    }

    private RideWaypoint persistWaypoint(Ride ride, int order, String address) {
        Location location = new Location();
        location.setAddress(address);
        location.setLat(BigDecimal.valueOf(45.20 + order));
        location.setLng(BigDecimal.valueOf(19.80 + order));
        location = locationRepository.saveAndFlush(location);

        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setLocation(location);
        waypoint.setWaypointOrder(order);
        return rideWaypointRepository.saveAndFlush(waypoint);
    }
}
