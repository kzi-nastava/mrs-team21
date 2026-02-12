package com.ftn.drumigo.event;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RidePassenger;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.RidePassengerRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RideFinishedEventListener {

    private static final Logger log = LoggerFactory.getLogger(RideFinishedEventListener.class);

    private final RideRepository rideRepository;
    private final RidePassengerRepository ridePassengerRepository;
    private final RideWaypointRepository rideWaypointRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRideFinished(RideFinishedEvent event) {
        Ride ride = rideRepository.findById(event.rideId()).orElse(null);
        if (ride == null) {
            log.warn("RideFinishedEvent received for missing ride id={}", event.rideId());
            return;
        }

        List<RidePassenger> linkedPassengers = ridePassengerRepository.findByRide(ride);
        Set<User> recipients = new LinkedHashSet<>();
        if (ride.getOrderingPassenger() != null) {
            recipients.add(ride.getOrderingPassenger());
        }
        // Add registered linked passengers
        for (RidePassenger ridePassenger : linkedPassengers) {
            userRepository.findByEmail(ridePassenger.getPassengerEmail()).ifPresent(recipients::add);
        }

        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        String startAddress = waypoints.isEmpty() ? "Unknown pickup" : waypoints.get(0).getLocation().getAddress();
        String destinationAddress = waypoints.isEmpty()
            ? "Unknown destination"
            : waypoints.get(waypoints.size() - 1).getLocation().getAddress();

        for (User user : recipients) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setRide(ride);
            notification.setType(NotificationType.RIDE_FINISHED);
            notification.setMessage("Ride finished. If you ordered this ride, you can rate the driver and vehicle.");
            notificationRepository.save(notification);

            boolean canRate = ride.getOrderingPassenger() != null
                && ride.getOrderingPassenger().getId().equals(user.getId());
            emailService.sendRideFinishedEmail(
                user.getEmail(),
                ride.getId(),
                startAddress,
                destinationAddress,
                canRate
            );
        }
    }
}
