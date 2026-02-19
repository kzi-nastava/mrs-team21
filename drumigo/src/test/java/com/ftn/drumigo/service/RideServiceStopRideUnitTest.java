package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.dto.ride.request.RideStopRequest;
import com.ftn.drumigo.dto.ride.request.EstimateRequest;
import com.ftn.drumigo.dto.ride.response.EstimateResponse;
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
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceStopRideUnitTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideWaypointRepository rideWaypointRepository;
    @Mock private RideInconsistencyRepository rideInconsistencyRepository;
    @Mock private RidePassengerRepository ridePassengerRepository;
    @Mock private PassengerRepository passengerRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleTypeRepository vehicleTypeRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private UserRepository userRepository;
    @Mock private PanicEventRepository panicEventRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MapService mapService;
    @Mock private AssignmentNotificationService assignmentNotificationService;

    @InjectMocks
    private RideService rideService;

    private Driver assignedDriver;
    private Ride activeRide;
    private RideStopRequest stopRequest;

    @BeforeEach
    void setUp() {
        assignedDriver = new Driver();
        assignedDriver.setId(77L);
        assignedDriver.setBusy(true);

        activeRide = new Ride();
        activeRide.setId(1001L);
        activeRide.setStatus(RideStatus.ACTIVE);
        activeRide.setDriver(assignedDriver);
        activeRide.setRequestedAt(Instant.now().minusSeconds(1200));
        activeRide.setStartTime(Instant.now().minusSeconds(900));
        activeRide.setPricingPricePerKm(new BigDecimal("100.00"));
        activeRide.setTotalCost(new BigDecimal("1000.00"));
        activeRide.setTotalDistanceKm(new BigDecimal("10.00"));

        Location pickupLocation = createLocation(6001L, "Pickup", new BigDecimal("45.25000000"), new BigDecimal("19.84000000"));
        Location destinationLocation = createLocation(6002L, "Destination", new BigDecimal("45.27000000"), new BigDecimal("19.90000000"));
        RideWaypoint pickupWaypoint = new RideWaypoint(7001L, activeRide, pickupLocation, 1);
        RideWaypoint destinationWaypoint = new RideWaypoint(7002L, activeRide, destinationLocation, 2);

        stopRequest = new RideStopRequest(
            "Stop Point 1",
            new BigDecimal("45.25100000"),
            new BigDecimal("19.84500000")
        );

        lenient().when(rideRepository.findById(activeRide.getId())).thenReturn(Optional.of(activeRide));
        lenient().when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(rideWaypointRepository.findByRideOrderByWaypointOrderAsc(activeRide))
            .thenReturn(List.of(pickupWaypoint, destinationWaypoint));
        lenient().when(rideWaypointRepository.findFirstByRideOrderByWaypointOrderDesc(activeRide))
            .thenReturn(Optional.of(destinationWaypoint));
        lenient().when(mapService.reverseGeocodeAddress(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldStopRideAndRecalculateCostAndDistance_whenActiveAndDriverAssigned() {
        Location existingStop = createLocation(501L, stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng());

        when(locationRepository.findByAddressAndLatAndLng(
            stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng()
        )).thenReturn(Optional.of(existingStop));
        when(mapService.estimateRide(any(EstimateRequest.class))).thenReturn(
            new EstimateResponse("poly", List.of(List.of(19.0, 45.0)), 5.0, 10, 500.0)
        );

        Ride stoppedRide = rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest);

        assertEquals(RideStatus.FINISHED, stoppedRide.getStatus());
        assertEquals(0, stoppedRide.getTotalCost().compareTo(new BigDecimal("500.00")));
        assertEquals(0, stoppedRide.getTotalDistanceKm().compareTo(new BigDecimal("5.00")));
        assertEquals(existingStop, stoppedRide.getStopLocation());
        assertNotNull(stoppedRide.getStoppedAt());
        assertNotNull(stoppedRide.getEndTime());
        assertNotNull(stoppedRide.getPaidAt());
        assertFalse(Boolean.TRUE.equals(assignedDriver.getBusy()));

        verify(locationRepository, never()).save(any(Location.class));
        verify(rideWaypointRepository).deleteByRideAndWaypointOrderGreaterThan(activeRide, 2);
        verify(rideWaypointRepository).save(any(RideWaypoint.class));
        verify(rideRepository).save(activeRide);
        verify(driverRepository).save(assignedDriver);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertInstanceOf(RideFinishedEvent.class, eventCaptor.getValue());
        assertEquals(activeRide.getId(), ((RideFinishedEvent) eventCaptor.getValue()).rideId());
    }

    @Test
    void shouldCreateStopLocation_whenLocationDoesNotExist() {
        when(locationRepository.findByAddressAndLatAndLng(
            stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng()
        )).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(777L);
            return saved;
        });

        Ride stoppedRide = rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest);

        assertNotNull(stoppedRide.getStopLocation());
        assertEquals(777L, stoppedRide.getStopLocation().getId());
        verify(locationRepository).save(any(Location.class));
        verify(mapService).estimateRide(any(EstimateRequest.class));
    }

    @Test
    void shouldClampCostAndDistanceToZero_whenRemainingIsGreaterThanOriginal() {
        activeRide.setTotalCost(new BigDecimal("100.00"));
        activeRide.setTotalDistanceKm(new BigDecimal("1.00"));
        activeRide.setPricingPricePerKm(new BigDecimal("200.00"));

        Location existingStop = createLocation(501L, stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng());

        when(locationRepository.findByAddressAndLatAndLng(
            stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng()
        )).thenReturn(Optional.of(existingStop));
        when(mapService.estimateRide(any(EstimateRequest.class))).thenReturn(
            new EstimateResponse("poly", List.of(), 2.0, 5, 100.0)
        );

        Ride stoppedRide = rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest);

        assertEquals(0, stoppedRide.getTotalCost().compareTo(new BigDecimal("400.00")));
        assertEquals(0, stoppedRide.getTotalDistanceKm().compareTo(new BigDecimal("2.00")));
    }

    @Test
    void shouldStillStopRide_whenMapServiceFails() {
        Location existingStop = createLocation(501L, stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng());
        BigDecimal originalCost = activeRide.getTotalCost();
        BigDecimal originalDistance = activeRide.getTotalDistanceKm();

        when(locationRepository.findByAddressAndLatAndLng(
            stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng()
        )).thenReturn(Optional.of(existingStop));
        when(mapService.estimateRide(any(EstimateRequest.class))).thenThrow(new RuntimeException("Map provider unavailable"));

        Ride stoppedRide = rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest);

        assertEquals(RideStatus.FINISHED, stoppedRide.getStatus());
        assertEquals(0, stoppedRide.getTotalCost().compareTo(originalCost));
        assertEquals(0, stoppedRide.getTotalDistanceKm().compareTo(originalDistance));
    }

    @Test
    void shouldUseReverseGeocodedAddress_whenAvailable() {
        when(mapService.reverseGeocodeAddress(stopRequest.stopLat(), stopRequest.stopLng()))
            .thenReturn(Optional.of("Reverse Geocoded Address"));
        when(locationRepository.findByAddressAndLatAndLng(
            "Reverse Geocoded Address", stopRequest.stopLat(), stopRequest.stopLng()
        )).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(333L);
            return saved;
        });

        Ride stoppedRide = rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest);

        assertEquals("Reverse Geocoded Address", stoppedRide.getStopLocation().getAddress());
    }

    @Test
    void shouldFallbackToProvidedStopAddress_whenReverseGeocodingReturnsEmpty() {
        when(mapService.reverseGeocodeAddress(stopRequest.stopLat(), stopRequest.stopLng()))
            .thenReturn(Optional.empty());
        when(locationRepository.findByAddressAndLatAndLng(
            stopRequest.stopAddress(), stopRequest.stopLat(), stopRequest.stopLng()
        )).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(444L);
            return saved;
        });

        Ride stoppedRide = rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest);

        assertEquals(stopRequest.stopAddress(), stoppedRide.getStopLocation().getAddress());
    }

    @Test
    void shouldThrowBadRequest_whenDriverIsNotAssignedToRide() {
        Driver anotherDriver = new Driver();
        anotherDriver.setId(88L);
        activeRide.setDriver(anotherDriver);

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest)
        );

        assertEquals("Driver is not assigned to this ride", ex.getMessage());
        verify(locationRepository, never()).findByAddressAndLatAndLng(any(), any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void shouldThrowBadRequest_whenRideIsNotActive() {
        activeRide.setStatus(RideStatus.ACCEPTED);

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> rideService.stopRide(activeRide.getId(), assignedDriver.getId(), stopRequest)
        );

        assertEquals("Ride must be ACTIVE to be stopped. Current status: ACCEPTED", ex.getMessage());
        verify(locationRepository, never()).findByAddressAndLatAndLng(any(), any(), any());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void shouldThrowResourceNotFound_whenRideDoesNotExist() {
        when(rideRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
            ResourceNotFoundException.class,
            () -> rideService.stopRide(404L, assignedDriver.getId(), stopRequest)
        );

        assertEquals("Ride not found with id: 404", ex.getMessage());
    }

    private Location createLocation(Long id, String address, BigDecimal lat, BigDecimal lng) {
        Location location = new Location();
        location.setId(id);
        location.setAddress(address);
        location.setLat(lat);
        location.setLng(lng);
        return location;
    }
}
