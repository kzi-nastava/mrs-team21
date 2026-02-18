package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.RideRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Sends reminder notifications for scheduled rides: 15 minutes before start,
 * then every 5 minutes (10 min, 5 min) until start.
 */
@Service
@RequiredArgsConstructor
public class ScheduledRideReminderService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRideReminderService.class);

    private static final int REMINDER_WINDOW_MINUTES = 15;
    private static final int[] REMINDER_MINUTES = { 15, 10, 5 };

    private final RideRepository rideRepository;
    private final RideWaypointRepository rideWaypointRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 60_000) // every minute
    @Transactional
    public void sendScheduledRideReminders() {
        Instant now = Instant.now();
        Instant windowEnd = now.plus(REMINDER_WINDOW_MINUTES, ChronoUnit.MINUTES);

        List<Ride> rides = rideRepository.findAcceptedScheduledRidesInReminderWindow(
            RideStatus.ACCEPTED,
            now,
            windowEnd
        );

        for (Ride ride : rides) {
            long secondsUntilStart = ChronoUnit.SECONDS.between(now, ride.getScheduledFor());
            if (secondsUntilStart <= 0) {
                continue;
            }
            // Send at most one reminder per run: the first one that is due and not yet sent.
            // "Due" = we are at or past the reminder time (secondsUntilStart <= reminderMin * 60).
            for (int reminderMin : REMINDER_MINUTES) {
                if (secondsUntilStart <= reminderMin * 60L) {
                    sendReminderIfNotSent(ride, reminderMin);
                    break;
                }
            }
        }
    }

    private void sendReminderIfNotSent(Ride ride, int minutesBefore) {
        if (notificationRepository.existsByRideAndTypeAndReminderMinutesBefore(
            ride, NotificationType.SCHEDULED_RIDE_REMINDER, minutesBefore)) {
            return;
        }

        String message = buildReminderMessage(ride, minutesBefore);
        Optional<User> recipient = collectRecipient(ride);
        if (recipient.isEmpty()) {
            return;
        }

        Notification n = new Notification();
        n.setUser(recipient.get());
        n.setRide(ride);
        n.setType(NotificationType.SCHEDULED_RIDE_REMINDER);
        n.setMessage(message);
        n.setReminderMinutes(minutesBefore);
        notificationRepository.save(n);

        log.info("Sent scheduled ride reminder: rideId={}, minutesBefore={}, recipientUserId={}",
            ride.getId(), minutesBefore, recipient.get().getId());
    }

    private String buildReminderMessage(Ride ride, int minutesBefore) {
        String pickup = "your pickup location";
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        if (!waypoints.isEmpty() && waypoints.get(0).getLocation() != null) {
            pickup = waypoints.get(0).getLocation().getAddress();
        }
        return String.format(
            "Reminder: Your scheduled ride #%d starts in %d minutes. Pickup: %s",
            ride.getId(),
            minutesBefore,
            pickup
        );
    }

    private Optional<User> collectRecipient(Ride ride) {
        return Optional.ofNullable(ride.getOrderingPassenger());
    }
}
