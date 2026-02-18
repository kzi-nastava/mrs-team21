package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.PanicEvent;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RideRepositoryTest {

    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private PassengerRepository passengerRepository;
    @Autowired
    private RidePassengerRepository ridePassengerRepository;
    @Autowired
    private PanicEventRepository panicEventRepository;

    @Test
    void findByStatus_returnsOnlyRidesWithMatchingStatus() {
        Driver driver = persistDriver("driver.status@mail.com", "Status", "Driver");
        Passenger passenger = persistPassenger("passenger.status@mail.com");
        persistRide(driver, passenger, RideStatus.ACTIVE, Instant.now(), null, Instant.now(), null);
        Ride finishedRide = persistRide(driver, passenger, RideStatus.FINISHED, Instant.now(), null, Instant.now(), Instant.now());

        List<Ride> finished = rideRepository.findByStatus(RideStatus.FINISHED);

        assertTrue(finished.stream().anyMatch(ride -> ride.getId().equals(finishedRide.getId())));
        assertTrue(finished.stream().allMatch(ride -> ride.getStatus() == RideStatus.FINISHED));
    }

    @Test
    void findByDriverAndStatus_andFindByDriverAndStatusIn_filterCorrectly() {
        Driver driverA = persistDriver("driver.a@mail.com", "Mark", "A");
        Driver driverB = persistDriver("driver.b@mail.com", "John", "B");
        Passenger passenger = persistPassenger("passenger.driver.status@mail.com");
        persistRide(driverA, passenger, RideStatus.ACTIVE, Instant.now(), null, Instant.now(), null);
        persistRide(driverA, passenger, RideStatus.ACCEPTED, Instant.now(), Instant.now().plusSeconds(300), null, null);
        persistRide(driverB, passenger, RideStatus.ACTIVE, Instant.now(), null, Instant.now(), null);

        List<Ride> driverAActive = rideRepository.findByDriverAndStatus(driverA, RideStatus.ACTIVE);
        List<Ride> driverAActiveOrAccepted = rideRepository.findByDriverAndStatusIn(
            driverA, List.of(RideStatus.ACTIVE, RideStatus.ACCEPTED)
        );

        assertEquals(1, driverAActive.size());
        assertEquals(RideStatus.ACTIVE, driverAActive.get(0).getStatus());
        assertEquals(2, driverAActiveOrAccepted.size());
    }

    @Test
    void findByDriverAndStatusAndScheduledForAfter_returnsPagedUpcomingAcceptedRides() {
        Driver driver = persistDriver("driver.scheduled@mail.com", "Sched", "Driver");
        Passenger passenger = persistPassenger("passenger.scheduled@mail.com");
        Instant now = Instant.now();

        Ride upcomingAccepted = persistRide(driver, passenger, RideStatus.ACCEPTED, now, now.plusSeconds(600), null, null);
        persistRide(driver, passenger, RideStatus.ACCEPTED, now, now.minusSeconds(600), null, null);
        persistRide(driver, passenger, RideStatus.ACTIVE, now, now.plusSeconds(600), now, null);

        Page<Ride> page = rideRepository.findByDriverAndStatusAndScheduledForAfter(
            driver, RideStatus.ACCEPTED, now, PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals(upcomingAccepted.getId(), page.getContent().get(0).getId());
    }

    @Test
    void findDriverRidesWithActivitySince_returnsRidesWithRecentActivity() {
        Driver driver = persistDriver("driver.activity@mail.com", "Activity", "Driver");
        Passenger passenger = persistPassenger("passenger.activity@mail.com");
        Instant now = Instant.now();
        Instant since = now.minusSeconds(3600);

        Ride activeWithoutEnd = persistRide(driver, passenger, RideStatus.ACTIVE, now.minusSeconds(2000), null, now.minusSeconds(1200), null);
        Ride recentlyFinished = persistRide(driver, passenger, RideStatus.FINISHED, now.minusSeconds(5000), null, now.minusSeconds(3000), now.minusSeconds(300));
        persistRide(driver, passenger, RideStatus.FINISHED, now.minusSeconds(10000), null, now.minusSeconds(9000), now.minusSeconds(8000));
        // Should be ignored because startTime is null.
        persistRide(driver, passenger, RideStatus.ACCEPTED, now.minusSeconds(400), now.plusSeconds(200), null, null);

        List<Ride> result = rideRepository.findDriverRidesWithActivitySince(driver, since);
        Set<Long> ids = result.stream().map(Ride::getId).collect(Collectors.toSet());

        assertEquals(Set.of(activeWithoutEnd.getId(), recentlyFinished.getId()), ids);
    }

    @Test
    void findByDriverAndRequestedAtBetween_returnsPagedRidesInRequestedInterval() {
        Driver driver = persistDriver("driver.requested.interval@mail.com", "Req", "Driver");
        Passenger passenger = persistPassenger("passenger.requested.interval@mail.com");
        Instant now = Instant.now();
        Ride inRange = persistRide(driver, passenger, RideStatus.FINISHED, now.minusSeconds(500), null, now.minusSeconds(400), now.minusSeconds(300));
        persistRide(driver, passenger, RideStatus.FINISHED, now.minusSeconds(5000), null, now.minusSeconds(4900), now.minusSeconds(4800));

        Page<Ride> result = rideRepository.findByDriverAndRequestedAtBetween(
            driver, now.minusSeconds(1000), now, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(inRange.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findByDriverNameContaining_matchesByNameOrSurname() {
        Driver matchByName = persistDriver("driver.name.match@mail.com", "Petar", "Ilic");
        Driver matchBySurname = persistDriver("driver.surname.match@mail.com", "Marko", "Petrovic");
        Passenger passenger = persistPassenger("passenger.driver.name@mail.com");
        persistRide(matchByName, passenger, RideStatus.FINISHED, Instant.now(), null, Instant.now(), Instant.now());
        persistRide(matchBySurname, passenger, RideStatus.FINISHED, Instant.now(), null, Instant.now(), Instant.now());

        List<Ride> result = rideRepository.findByDriverNameContaining("Pet");

        assertEquals(2, result.size());
    }

    @Test
    void findByStatusAndRequestedAtBetween_filtersByStatusAndInterval() {
        Driver driver = persistDriver("driver.status.interval@mail.com", "Status", "Interval");
        Passenger passenger = persistPassenger("passenger.status.interval@mail.com");
        Instant now = Instant.now();
        Ride wanted = persistRide(driver, passenger, RideStatus.FINISHED, now.minusSeconds(700), null, now.minusSeconds(600), now.minusSeconds(500));
        persistRide(driver, passenger, RideStatus.CANCELLED, now.minusSeconds(700), null, now.minusSeconds(600), now.minusSeconds(500));

        List<Ride> result = rideRepository.findByStatusAndRequestedAtBetween(
            RideStatus.FINISHED, now.minusSeconds(1000), now
        );

        assertEquals(1, result.size());
        assertEquals(wanted.getId(), result.get(0).getId());
    }

    @Test
    void findByStatusAndUserAndRequestedAtBetween_matchesOrderingOrLinkedPassenger() {
        Driver driver = persistDriver("driver.status.user@mail.com", "StatusUser", "Driver");
        Passenger orderingPassenger = persistPassenger("ordering.status.user@mail.com");
        Passenger linkedRegisteredPassenger = persistPassenger("linked.status.user@mail.com");
        Instant now = Instant.now();

        Ride orderingRide = persistRide(driver, orderingPassenger, RideStatus.FINISHED, now.minusSeconds(300), null, now.minusSeconds(200), now.minusSeconds(100));
        Ride linkedRide = persistRide(driver, orderingPassenger, RideStatus.FINISHED, now.minusSeconds(400), null, now.minusSeconds(300), now.minusSeconds(200));
        ridePassengerRepository.saveAndFlush(new RidePassenger(linkedRide, linkedRegisteredPassenger.getEmail()));

        List<Ride> result = rideRepository.findByStatusAndUserAndRequestedAtBetween(
            RideStatus.FINISHED,
            linkedRegisteredPassenger.getId(),
            linkedRegisteredPassenger.getEmail(),
            now.minusSeconds(1000),
            now
        );

        assertEquals(1, result.size());
        assertEquals(linkedRide.getId(), result.get(0).getId());
        assertFalse(result.stream().anyMatch(r -> r.getId().equals(orderingRide.getId())));
    }

    @Test
    void findPassengerHistory_filtersByStatusAndPanic() {
        Driver driver = persistDriver("driver.passenger.history@mail.com", "Passenger", "History");
        Passenger orderingPassenger = persistPassenger("ordering.passenger.history@mail.com");
        Passenger linkedPassenger = persistPassenger("linked.passenger.history@mail.com");
        Instant now = Instant.now();

        Ride panicRide = persistRide(driver, orderingPassenger, RideStatus.FINISHED, now.minusSeconds(300), null, now.minusSeconds(200), now.minusSeconds(100));
        ridePassengerRepository.saveAndFlush(new RidePassenger(panicRide, linkedPassenger.getEmail()));
        PanicEvent panic = new PanicEvent();
        panic.setRide(panicRide);
        panic.setUser(linkedPassenger);
        panicEventRepository.saveAndFlush(panic);

        Ride noPanicRide = persistRide(driver, orderingPassenger, RideStatus.FINISHED, now.minusSeconds(500), null, now.minusSeconds(400), now.minusSeconds(300));
        ridePassengerRepository.saveAndFlush(new RidePassenger(noPanicRide, linkedPassenger.getEmail()));

        Page<Ride> panicOnly = rideRepository.findPassengerHistory(
            linkedPassenger.getId(),
            linkedPassenger.getEmail(),
            now.minusSeconds(1000),
            now,
            List.of(RideStatus.FINISHED),
            true,
            PageRequest.of(0, 10)
        );

        assertEquals(1, panicOnly.getTotalElements());
        assertEquals(panicRide.getId(), panicOnly.getContent().get(0).getId());
    }

    @Test
    void findAdminRideHistory_filtersByStatusesAndPanicFlag() {
        Driver driver = persistDriver("driver.admin.history@mail.com", "Admin", "History");
        Passenger passenger = persistPassenger("passenger.admin.history@mail.com");
        Instant now = Instant.now();

        Ride finishedWithPanic = persistRide(driver, passenger, RideStatus.FINISHED, now.minusSeconds(300), null, now.minusSeconds(200), now.minusSeconds(100));
        PanicEvent panic = new PanicEvent();
        panic.setRide(finishedWithPanic);
        panic.setUser(passenger);
        panicEventRepository.saveAndFlush(panic);

        persistRide(driver, passenger, RideStatus.CANCELLED, now.minusSeconds(400), null, now.minusSeconds(350), now.minusSeconds(300));

        Page<Ride> result = rideRepository.findAdminRideHistory(
            now.minusSeconds(1000),
            now,
            List.of(RideStatus.FINISHED),
            true,
            PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(finishedWithPanic.getId(), result.getContent().get(0).getId());
    }

    @Test
    void existsActiveRideForPassenger_checksOrderingAndLinkedPassenger() {
        Driver driver = persistDriver("driver.active.exists@mail.com", "Active", "Exists");
        Passenger orderingPassenger = persistPassenger("ordering.active.exists@mail.com");
        Passenger linkedPassenger = persistPassenger("linked.active.exists@mail.com");

        Ride activeRide = persistRide(driver, orderingPassenger, RideStatus.ACTIVE, Instant.now(), null, Instant.now(), null);
        ridePassengerRepository.saveAndFlush(new RidePassenger(activeRide, linkedPassenger.getEmail()));

        assertTrue(rideRepository.existsActiveRideForPassenger(
            orderingPassenger.getId(),
            orderingPassenger.getEmail(),
            List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE)
        ));

        assertTrue(rideRepository.existsActiveRideForPassenger(
            linkedPassenger.getId(),
            linkedPassenger.getEmail(),
            List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE)
        ));

        assertFalse(rideRepository.existsActiveRideForPassenger(
            999999L,
            "nobody@mail.com",
            List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE)
        ));
    }

    @Test
    void findAcceptedScheduledRidesInReminderWindow_returnsOnlyRidesWithinWindow() {
        Driver driver = persistDriver("driver.reminder.window@mail.com", "Reminder", "Driver");
        Passenger passenger = persistPassenger("passenger.reminder.window@mail.com");
        Instant now = Instant.now();
        Ride inWindow = persistRide(driver, passenger, RideStatus.ACCEPTED, now.minusSeconds(60), now.plusSeconds(600), null, null);
        persistRide(driver, passenger, RideStatus.ACCEPTED, now.minusSeconds(60), now.plusSeconds(2000), null, null);
        persistRide(driver, passenger, RideStatus.ACTIVE, now.minusSeconds(60), now.plusSeconds(500), now, null);

        List<Ride> result = rideRepository.findAcceptedScheduledRidesInReminderWindow(
            RideStatus.ACCEPTED,
            now,
            now.plusSeconds(900)
        );

        assertEquals(1, result.size());
        assertEquals(inWindow.getId(), result.get(0).getId());
    }

    private Driver persistDriver(String email, String name, String surname) {
        Driver driver = new Driver();
        driver.setName(name);
        driver.setSurname(surname);
        driver.setEmail(email);
        driver.setActive(true);
        driver.setBlocked(false);
        driver.setRole(UserRole.DRIVER);
        driver.setActiveDriver(true);
        driver.setBusy(false);
        return driverRepository.saveAndFlush(driver);
    }

    private Passenger persistPassenger(String email) {
        Passenger passenger = new Passenger();
        passenger.setName("Passenger");
        passenger.setSurname("Tester");
        passenger.setEmail(email);
        passenger.setActive(true);
        passenger.setBlocked(false);
        passenger.setRole(UserRole.PASSENGER);
        return passengerRepository.saveAndFlush(passenger);
    }

    private Ride persistRide(
        Driver driver,
        Passenger orderingPassenger,
        RideStatus status,
        Instant requestedAt,
        Instant scheduledFor,
        Instant startTime,
        Instant endTime
    ) {
        Ride ride = new Ride();
        ride.setStatus(status);
        ride.setRequestedAt(requestedAt);
        ride.setScheduledFor(scheduledFor);
        ride.setStartTime(startTime);
        ride.setEndTime(endTime);
        ride.setDriver(driver);
        ride.setOrderingPassenger(orderingPassenger);
        ride.setBabyTransport(false);
        ride.setPetTransport(false);
        return rideRepository.saveAndFlush(ride);
    }
}
