package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.event.RideFinishedEvent;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.repository.DriverRepository;
import com.ftn.drumigo.repository.LocationRepository;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.PanicEventRepository;
import com.ftn.drumigo.repository.PassengerRepository;
import com.ftn.drumigo.repository.ReviewRepository;
import com.ftn.drumigo.repository.RideInconsistencyRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.repository.VehicleRepository;
import com.ftn.drumigo.repository.VehicleTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;
    @Mock
    private RideWaypointRepository rideWaypointRepository;
    @Mock
    private RideInconsistencyRepository rideInconsistencyRepository;
    @Mock
    private RidePassengerRepository ridePassengerRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleTypeRepository vehicleTypeRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PanicEventRepository panicEventRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MapService mapService;
    @Mock
    private AssignmentNotificationService assignmentNotificationService;

    @InjectMocks
    private RideService rideService;

    @Test
    void endRide_whenRideIsActive_marksFinishedAndPublishesEvent() {
        Driver driver = driver(20L, true);
        Ride ride = ride(10L, RideStatus.ACTIVE, driver);
        Instant before = Instant.now();

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = rideService.endRide(10L);

        Instant after = Instant.now();
        assertEquals(RideStatus.FINISHED, result.getStatus());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getPaidAt());
        assertFalse(driver.isBusy());
        assertFalse(result.getEndTime().isBefore(before));
        assertFalse(result.getEndTime().isAfter(after));
        assertFalse(result.getPaidAt().isBefore(before));
        assertFalse(result.getPaidAt().isAfter(after));

        verify(driverRepository).save(driver);
        verify(rideRepository).save(ride);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        RideFinishedEvent event = assertInstanceOf(RideFinishedEvent.class, eventCaptor.getValue());
        assertEquals(10L, event.rideId());
    }

    @Test
    void endRide_whenRideStatusIsNotActive_throwsBadRequest() {
        Ride ride = ride(11L, RideStatus.ACCEPTED, driver(21L, true));
        when(rideRepository.findById(11L)).thenReturn(Optional.of(ride));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> rideService.endRide(11L));

        assertEquals("Ride must be ACTIVE to be ended. Current status: ACCEPTED", ex.getMessage());
        verify(rideRepository, never()).save(any(Ride.class));
        verify(driverRepository, never()).save(any(Driver.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void endRide_whenRideMissing_throwsResourceNotFound() {
        when(rideRepository.findById(12L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> rideService.endRide(12L));

        assertEquals("Ride not found with id: 12", ex.getMessage());
        verify(rideRepository, never()).save(any(Ride.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void endRideByDriverId_whenDriverMissing_throwsResourceNotFound() {
        when(driverRepository.findById(30L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
            ResourceNotFoundException.class,
            () -> rideService.endRideByDriverId(15L, 30L)
        );

        assertEquals("Driver not found with ID: 30", ex.getMessage());
        verify(rideRepository, never()).findById(any());
        verify(rideRepository, never()).save(any(Ride.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void endRideByDriverId_whenRideHasNoDriver_throwsBadRequest() {
        Driver authenticatedDriver = driver(31L, true);
        Ride ride = ride(16L, RideStatus.ACTIVE, null);
        when(driverRepository.findById(31L)).thenReturn(Optional.of(authenticatedDriver));
        when(rideRepository.findById(16L)).thenReturn(Optional.of(ride));

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> rideService.endRideByDriverId(16L, 31L)
        );

        assertEquals("Driver is not assigned to this ride", ex.getMessage());
        verify(rideRepository, never()).save(any(Ride.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void endRideByDriverId_whenDriverDoesNotMatchRide_throwsBadRequest() {
        Driver authenticatedDriver = driver(32L, true);
        Driver anotherDriver = driver(99L, true);
        Ride ride = ride(17L, RideStatus.ACTIVE, anotherDriver);
        when(driverRepository.findById(32L)).thenReturn(Optional.of(authenticatedDriver));
        when(rideRepository.findById(17L)).thenReturn(Optional.of(ride));

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> rideService.endRideByDriverId(17L, 32L)
        );

        assertEquals("Driver is not assigned to this ride", ex.getMessage());
        verify(rideRepository, never()).save(any(Ride.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void endRideByDriverId_whenAssignedDriverAndRideActive_endsRide() {
        Driver authenticatedDriver = driver(33L, true);
        Ride ride = ride(18L, RideStatus.ACTIVE, authenticatedDriver);
        when(driverRepository.findById(33L)).thenReturn(Optional.of(authenticatedDriver));
        when(rideRepository.findById(18L)).thenReturn(Optional.of(ride));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = rideService.endRideByDriverId(18L, 33L);

        assertEquals(RideStatus.FINISHED, result.getStatus());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getPaidAt());
        assertFalse(authenticatedDriver.isBusy());
        verify(rideRepository).save(ride);
        verify(eventPublisher).publishEvent(any(RideFinishedEvent.class));
    }

    @Test
    void getMyActiveRide_passenger_ignoresFutureScheduledRide() {
        Passenger passenger = passenger(100L, "ana.petrovic@example.com");
        Ride futureAcceptedRide = ride(200L, RideStatus.ACCEPTED, null);
        futureAcceptedRide.setScheduledFor(Instant.now().plusSeconds(3 * 60 * 60));

        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(rideRepository.findActiveRidesForPassenger(
            100L,
            "ana.petrovic@example.com",
            List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE)
        )).thenReturn(List.of(futureAcceptedRide));

        Optional<Ride> result = rideService.getMyActiveRide(100L, "PASSENGER");

        assertTrue(result.isEmpty());
    }

    @Test
    void getMyActiveRide_passenger_returnsTrackableRideAndSkipsFutureScheduled() {
        Passenger passenger = passenger(101L, "ana.petrovic@example.com");
        Ride futureAcceptedRide = ride(201L, RideStatus.ACCEPTED, null);
        futureAcceptedRide.setScheduledFor(Instant.now().plusSeconds(2 * 60 * 60));
        Ride activeRide = ride(202L, RideStatus.ACTIVE, null);
        activeRide.setScheduledFor(Instant.now().plusSeconds(2 * 60 * 60));

        when(passengerRepository.findById(101L)).thenReturn(Optional.of(passenger));
        when(rideRepository.findActiveRidesForPassenger(
            101L,
            "ana.petrovic@example.com",
            List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE)
        )).thenReturn(List.of(futureAcceptedRide, activeRide));

        Optional<Ride> result = rideService.getMyActiveRide(101L, "PASSENGER");

        assertTrue(result.isPresent());
        assertEquals(202L, result.get().getId());
    }

    @Test
    void getMyActiveRide_driver_ignoresFutureScheduledAcceptedRide() {
        Driver driver = driver(110L, false);
        Ride futureAcceptedRide = ride(210L, RideStatus.ACCEPTED, driver);
        futureAcceptedRide.setScheduledFor(Instant.now().plusSeconds(4 * 60 * 60));

        when(driverRepository.findById(110L)).thenReturn(Optional.of(driver));
        when(rideRepository.findByDriverAndStatusIn(
            driver,
            List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE)
        )).thenReturn(List.of(futureAcceptedRide));

        Optional<Ride> result = rideService.getMyActiveRide(110L, "DRIVER");

        assertTrue(result.isEmpty());
    }

    private static Ride ride(Long id, RideStatus status, Driver driver) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setStatus(status);
        ride.setDriver(driver);
        return ride;
    }

    private static Driver driver(Long id, boolean busy) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setBusy(busy);
        return driver;
    }

    private static Passenger passenger(Long id, String email) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setEmail(email);
        return passenger;
    }
}
