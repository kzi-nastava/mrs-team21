package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.enums.VehicleTypeName;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.dto.RideCreateRequest;
import com.ftn.drumigo.dto.ride.response.EstimateResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceCreateRideTest {

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
    void create_whenPassengerHasActiveRide_throwsBadRequest() {
        Passenger passenger = passenger(10L, "orderer@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, List.of(), false, false);
        when(passengerRepository.findById(10L)).thenReturn(Optional.of(passenger));
        when(rideRepository.existsActiveRideForPassenger(10L, "orderer@mail.com",
            List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.ACTIVE))).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> rideService.create(10L, request));

        assertEquals("Cannot create a new ride while you have an active ride. Please wait until your current ride is finished.", ex.getMessage());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void create_whenWaypointsLessThanTwo_throwsBadRequest() {
        Passenger passenger = passenger(11L, "orderer@mail.com");
        RideCreateRequest request = new RideCreateRequest(
            List.of(new RideCreateRequest.WaypointRequest("Only one", BigDecimal.valueOf(45.2), BigDecimal.valueOf(19.8), 1)),
            VehicleTypeName.STANDARD,
            false,
            false,
            List.of(),
            null
        );
        when(passengerRepository.findById(11L)).thenReturn(Optional.of(passenger));
        when(rideRepository.existsActiveRideForPassenger(any(), any(), anyList())).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> rideService.create(11L, request));

        assertEquals("Ride must have at least 2 waypoints (start and destination)", ex.getMessage());
    }

    @Test
    void create_whenScheduledMoreThanFiveHoursAhead_throwsBadRequest() {
        Passenger passenger = passenger(12L, "orderer@mail.com");
        Instant tooLate = Instant.now().plusSeconds(6 * 3600);
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, tooLate, List.of(), false, false);
        when(passengerRepository.findById(12L)).thenReturn(Optional.of(passenger));
        when(rideRepository.existsActiveRideForPassenger(any(), any(), anyList())).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> rideService.create(12L, request));

        assertEquals("Ride cannot be scheduled more than 5 hours in advance", ex.getMessage());
    }

    @Test
    void create_whenVehicleTypeMissing_throwsResourceNotFound() {
        Passenger passenger = passenger(13L, "orderer@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, List.of(), false, false);
        when(passengerRepository.findById(13L)).thenReturn(Optional.of(passenger));
        when(rideRepository.existsActiveRideForPassenger(any(), any(), anyList())).thenReturn(false);
        when(vehicleTypeRepository.findByName(VehicleTypeName.STANDARD)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> rideService.create(13L, request));

        assertEquals("Vehicle type not found: STANDARD", ex.getMessage());
    }

    @Test
    void create_whenNoActiveDrivers_rejectsRideAndCreatesRejectionNotification() {
        Passenger passenger = passenger(14L, "orderer@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, List.of(), false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of());

        Ride result = rideService.create(14L, request);

        assertEquals(RideStatus.REJECTED, result.getStatus());
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification rejection = notificationCaptor.getValue();
        assertEquals(NotificationType.RIDE_REJECTED, rejection.getType());
        assertEquals("Your ride request has been rejected. There are currently no active drivers.", rejection.getMessage());
    }

    @Test
    void create_whenFreeEligibleDriverExists_assignsDriverAndMarksBusy() {
        Passenger passenger = passenger(15L, "orderer@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, List.of(), false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        Driver driver = driver(20L, true, false);
        Vehicle vehicle = vehicle(driver, vehicleType, true, true, BigDecimal.valueOf(45.255), BigDecimal.valueOf(19.845));

        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of(driver));
        when(rideRepository.findDriverRidesWithActivitySince(eq(driver), any())).thenReturn(List.of());
        when(vehicleRepository.findByDriver(driver)).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverAndStatusIn(eq(driver), anyList())).thenReturn(List.of());

        Ride result = rideService.create(15L, request);

        assertEquals(RideStatus.ACCEPTED, result.getStatus());
        assertEquals(driver.getId(), result.getDriver().getId());
        verify(driverRepository).save(driver);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void create_whenScheduledRideInFuture_doesNotMarkDriverBusyImmediately() {
        Passenger passenger = passenger(16L, "orderer@mail.com");
        Instant scheduledFor = Instant.now().plusSeconds(3600);
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, scheduledFor, List.of(), false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        Driver driver = driver(21L, true, false);
        Vehicle vehicle = vehicle(driver, vehicleType, true, true, BigDecimal.valueOf(45.255), BigDecimal.valueOf(19.845));

        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of(driver));
        when(rideRepository.findDriverRidesWithActivitySince(eq(driver), any())).thenReturn(List.of());
        when(vehicleRepository.findByDriver(driver)).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverAndStatusIn(eq(driver), anyList())).thenReturn(List.of());

        Ride result = rideService.create(16L, request);

        assertEquals(RideStatus.ACCEPTED, result.getStatus());
        verify(driverRepository, never()).save(driver);
    }

    @Test
    void create_whenFallbackBusyDriverFinishingWithinTenMinutes_assignsFallbackDriver() {
        Passenger passenger = passenger(17L, "orderer@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, List.of(), false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        Driver busyDriver = driver(22L, true, true);
        Vehicle vehicle = vehicle(busyDriver, vehicleType, true, true, BigDecimal.valueOf(45.255), BigDecimal.valueOf(19.845));
        Ride activeAssignment = new Ride();
        activeAssignment.setStatus(RideStatus.ACTIVE);
        activeAssignment.setStartTime(Instant.now().minusSeconds(300));
        activeAssignment.setEstimatedArrivalAt(Instant.now().plusSeconds(300));
        activeAssignment.setEstimatedDurationSec(900);

        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of(busyDriver));
        when(rideRepository.findDriverRidesWithActivitySince(eq(busyDriver), any())).thenReturn(List.of());
        when(vehicleRepository.findByDriver(busyDriver)).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverAndStatusIn(eq(busyDriver), anyList())).thenReturn(List.of(activeAssignment));

        Ride result = rideService.create(17L, request);

        assertEquals(RideStatus.ACCEPTED, result.getStatus());
        assertEquals(busyDriver.getId(), result.getDriver().getId());
    }

    @Test
    void create_whenLinkedEmailsContainOrderingPassenger_throwsBadRequest() {
        Passenger passenger = passenger(18L, "orderer@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, List.of("orderer@mail.com"), false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        Driver driver = driver(23L, true, false);
        Vehicle vehicle = vehicle(driver, vehicleType, true, true, BigDecimal.valueOf(45.255), BigDecimal.valueOf(19.845));

        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of(driver));
        when(rideRepository.findDriverRidesWithActivitySince(eq(driver), any())).thenReturn(List.of());
        when(vehicleRepository.findByDriver(driver)).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverAndStatusIn(eq(driver), anyList())).thenReturn(List.of());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> rideService.create(18L, request));

        assertEquals("Cannot link the ordering passenger to their own ride", ex.getMessage());
    }

    @Test
    void create_whenAcceptedWithLinkedPassengers_savesRidePassengersAndNotifiesAccepted() {
        Passenger passenger = passenger(19L, "orderer@mail.com");
        List<String> linked = List.of("friend1@mail.com", "friend2@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, linked, false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        Driver driver = driver(24L, true, false);
        Vehicle vehicle = vehicle(driver, vehicleType, true, true, BigDecimal.valueOf(45.255), BigDecimal.valueOf(19.845));

        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of(driver));
        when(rideRepository.findDriverRidesWithActivitySince(eq(driver), any())).thenReturn(List.of());
        when(vehicleRepository.findByDriver(driver)).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverAndStatusIn(eq(driver), anyList())).thenReturn(List.of());
        when(ridePassengerRepository.save(any(RidePassenger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = rideService.create(19L, request);

        assertEquals(RideStatus.ACCEPTED, result.getStatus());
        verify(ridePassengerRepository, times(2)).save(any(RidePassenger.class));
        verify(assignmentNotificationService).notifyLinkedPassengersAccepted(result, linked);
        verify(assignmentNotificationService, never()).notifyLinkedPassengersRejected(any(), any(), anyList());
    }

    @Test
    void create_whenRejectedWithLinkedPassengers_notifiesRejectedAndDoesNotSaveRidePassengers() {
        Passenger passenger = passenger(20L, "orderer@mail.com");
        List<String> linked = List.of("friend1@mail.com");
        RideCreateRequest request = validRequest(VehicleTypeName.STANDARD, null, linked, false, false);
        VehicleType vehicleType = vehicleType(1L, VehicleTypeName.STANDARD);
        configureCreateBase(passenger, vehicleType);
        when(driverRepository.findByActiveDriverTrue()).thenReturn(List.of());

        Ride result = rideService.create(20L, request);

        assertEquals(RideStatus.REJECTED, result.getStatus());
        verify(ridePassengerRepository, never()).save(any(RidePassenger.class));
        verify(assignmentNotificationService).notifyLinkedPassengersRejected(
            eq(result),
            eq("There are currently no active drivers."),
            eq(linked)
        );
    }

    private void configureCreateBase(Passenger passenger, VehicleType vehicleType) {
        when(passengerRepository.findById(passenger.getId())).thenReturn(Optional.of(passenger));
        when(rideRepository.existsActiveRideForPassenger(eq(passenger.getId()), eq(passenger.getEmail()), anyList())).thenReturn(false);
        when(vehicleTypeRepository.findByName(vehicleType.getName())).thenReturn(Optional.of(vehicleType));
        when(mapService.estimateRide(any())).thenReturn(new EstimateResponse("polyline", List.of(), 6.5, 12, 900.0));

        AtomicLong rideIdCounter = new AtomicLong(1000);
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            if (ride.getId() == null) {
                ride.setId(rideIdCounter.getAndIncrement());
            }
            return ride;
        });

        AtomicLong locationIdCounter = new AtomicLong(2000);
        when(locationRepository.findByAddressAndLatAndLng(any(), any(), any())).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            if (location.getId() == null) {
                location.setId(locationIdCounter.getAndIncrement());
            }
            return location;
        });
        when(rideWaypointRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static RideCreateRequest validRequest(
        VehicleTypeName vehicleTypeName,
        Instant scheduledFor,
        List<String> linkedEmails,
        Boolean babyTransport,
        Boolean petTransport
    ) {
        return new RideCreateRequest(
            List.of(
                new RideCreateRequest.WaypointRequest("Bulevar Oslobodjenja 1", BigDecimal.valueOf(45.2551), BigDecimal.valueOf(19.8452), 1),
                new RideCreateRequest.WaypointRequest("Narodnog Fronta 10", BigDecimal.valueOf(45.2466), BigDecimal.valueOf(19.8369), 2)
            ),
            vehicleTypeName,
            babyTransport,
            petTransport,
            linkedEmails,
            scheduledFor
        );
    }

    private static Passenger passenger(Long id, String email) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setEmail(email);
        passenger.setName("Passenger");
        passenger.setSurname("Tester");
        passenger.setRole(UserRole.PASSENGER);
        passenger.setActive(true);
        passenger.setBlocked(false);
        return passenger;
    }

    private static Driver driver(Long id, boolean activeDriver, boolean busy) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setEmail("driver" + id + "@mail.com");
        driver.setName("Driver");
        driver.setSurname("Tester");
        driver.setRole(UserRole.DRIVER);
        driver.setActive(true);
        driver.setBlocked(false);
        driver.setActiveDriver(activeDriver);
        driver.setBusy(busy);
        return driver;
    }

    private static VehicleType vehicleType(Long id, VehicleTypeName name) {
        VehicleType type = new VehicleType();
        type.setId(id);
        type.setName(name);
        type.setStartPrice(BigDecimal.valueOf(200));
        type.setPricePerKm(BigDecimal.valueOf(120));
        return type;
    }

    private static Vehicle vehicle(
        Driver driver,
        VehicleType vehicleType,
        Boolean babyFriendly,
        Boolean petFriendly,
        BigDecimal lat,
        BigDecimal lng
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(driver.getId() + 100);
        vehicle.setDriver(driver);
        vehicle.setVehicleType(vehicleType);
        vehicle.setLicensePlate("NS-" + driver.getId() + "-TS");
        vehicle.setModel("Model");
        vehicle.setNumSeats(4);
        vehicle.setBabyFriendly(babyFriendly);
        vehicle.setPetFriendly(petFriendly);
        vehicle.setCurrentLat(lat);
        vehicle.setCurrentLng(lng);
        return vehicle;
    }
}