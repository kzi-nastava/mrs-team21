package com.ftn.drumigo.event;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideFinishedEventListenerTest {

    @Mock
    private RideRepository rideRepository;
    @Mock
    private RidePassengerRepository ridePassengerRepository;
    @Mock
    private RideWaypointRepository rideWaypointRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private RideFinishedEventListener listener;

    @Test
    void handleRideFinished_whenRideMissing_doesNothing() {
        when(rideRepository.findById(500L)).thenReturn(Optional.empty());

        listener.handleRideFinished(new RideFinishedEvent(500L));

        verify(ridePassengerRepository, never()).findByRide(any(Ride.class));
        verify(rideWaypointRepository, never()).findByRideOrderByWaypointOrderAsc(any(Ride.class));
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(emailService, never()).sendRideFinishedEmail(anyString(), anyLong(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void handleRideFinished_whenRecipientsExist_createsNotificationsAndEmailsWithCorrectCanRate() {
        Passenger orderingPassenger = passenger(101L, "order@test.com");
        Passenger linkedPassenger = passenger(202L, "linked@test.com");

        Ride ride = new Ride();
        ride.setId(20L);
        ride.setOrderingPassenger(orderingPassenger);

        RidePassenger registeredLinked = new RidePassenger(ride, linkedPassenger.getEmail());
        RidePassenger unregisteredLinked = new RidePassenger(ride, "guest@test.com");

        when(rideRepository.findById(20L)).thenReturn(Optional.of(ride));
        when(ridePassengerRepository.findByRide(ride)).thenReturn(List.of(registeredLinked, unregisteredLinked));
        when(userRepository.findByEmail(linkedPassenger.getEmail())).thenReturn(Optional.of(linkedPassenger));
        when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.empty());
        when(rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride)).thenReturn(
            List.of(
                waypoint(ride, 0, "Pickup Street 1"),
                waypoint(ride, 1, "Destination Street 9")
            )
        );

        listener.handleRideFinished(new RideFinishedEvent(20L));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Set<String> notifiedEmails = notificationCaptor.getAllValues()
            .stream()
            .map(notification -> notification.getUser().getEmail())
            .collect(Collectors.toSet());
        assertEquals(Set.of("order@test.com", "linked@test.com"), notifiedEmails);
        assertTrue(notificationCaptor.getAllValues().stream().allMatch(notification -> notification.getType() == NotificationType.RIDE_FINISHED));

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> rideIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> pickupCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> canRateCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(emailService, times(2)).sendRideFinishedEmail(
            emailCaptor.capture(),
            rideIdCaptor.capture(),
            pickupCaptor.capture(),
            destinationCaptor.capture(),
            canRateCaptor.capture()
        );

        Map<String, Boolean> canRateByEmail = new HashMap<>();
        for (int i = 0; i < emailCaptor.getAllValues().size(); i++) {
            canRateByEmail.put(emailCaptor.getAllValues().get(i), canRateCaptor.getAllValues().get(i));
            assertEquals(20L, rideIdCaptor.getAllValues().get(i));
            assertEquals("Pickup Street 1", pickupCaptor.getAllValues().get(i));
            assertEquals("Destination Street 9", destinationCaptor.getAllValues().get(i));
        }
        assertEquals(Boolean.TRUE, canRateByEmail.get("order@test.com"));
        assertEquals(Boolean.FALSE, canRateByEmail.get("linked@test.com"));
    }

    @Test
    void handleRideFinished_whenWaypointsMissing_usesUnknownAddresses() {
        Passenger orderingPassenger = passenger(303L, "ordering@test.com");
        Ride ride = new Ride();
        ride.setId(30L);
        ride.setOrderingPassenger(orderingPassenger);

        when(rideRepository.findById(30L)).thenReturn(Optional.of(ride));
        when(ridePassengerRepository.findByRide(ride)).thenReturn(List.of());
        when(rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride)).thenReturn(List.of());

        listener.handleRideFinished(new RideFinishedEvent(30L));

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailService).sendRideFinishedEmail(
            eq("ordering@test.com"),
            eq(30L),
            eq("Unknown pickup"),
            eq("Unknown destination"),
            eq(true)
        );
    }

    private static Passenger passenger(Long id, String email) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setEmail(email);
        return passenger;
    }

    private static RideWaypoint waypoint(Ride ride, int order, String address) {
        Location location = new Location();
        location.setAddress(address);

        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setWaypointOrder(order);
        waypoint.setLocation(location);
        return waypoint;
    }
}
