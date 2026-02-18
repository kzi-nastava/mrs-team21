package com.ftn.drumigo.service;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.repository.NotificationRepository;
import com.ftn.drumigo.repository.RideWaypointRepository;
import com.ftn.drumigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles assignment-related notifications (driver assignment, linked passengers).
 * Single responsibility: sending notifications and emails when a ride is accepted or rejected.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentNotificationService {

    private static final String LINKED_ACCEPTED_MESSAGE =
            "You have been added to a ride and it has been accepted. You can track it in the app.";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RideWaypointRepository rideWaypointRepository;
    private final EmailService emailService;

    /**
     * Sends notification and email to each linked passenger when a ride is accepted.
     * Registered users receive in-app notification; all linked emails receive email with tracking link.
     */
    public void notifyLinkedPassengersAccepted(Ride ride, List<String> linkedPassengerEmails) {
        if (linkedPassengerEmails == null || linkedPassengerEmails.isEmpty()) {
            return;
        }
        String pickupAddress = resolvePickupAddress(ride);
        String destinationAddress = resolveDestinationAddress(ride);
        for (String email : linkedPassengerEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setRide(ride);
                notification.setType(NotificationType.LINKED_TO_RIDE);
                notification.setMessage(LINKED_ACCEPTED_MESSAGE);
                notificationRepository.save(notification);
            });
            emailService.sendLinkedPassengerRideAcceptedEmail(
                    email, ride.getId(), pickupAddress, destinationAddress);
        }
    }

    /**
     * Sends notification and email to each linked passenger when a ride is rejected.
     * Registered users receive in-app notification; all linked emails receive email.
     */
    public void notifyLinkedPassengersRejected(Ride ride, String reason, List<String> linkedPassengerEmails) {
        if (linkedPassengerEmails == null || linkedPassengerEmails.isEmpty()) {
            return;
        }
        for (String email : linkedPassengerEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setRide(ride);
                notification.setType(NotificationType.RIDE_REJECTED);
                notification.setMessage("The ride you were linked to has been rejected. " + reason);
                notificationRepository.save(notification);
            });
            emailService.sendLinkedPassengerRideRejectedEmail(email, reason);
        }
    }

    private String resolvePickupAddress(Ride ride) {
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        return waypoints.isEmpty() ? "Unknown pickup" : waypoints.get(0).getLocation().getAddress();
    }

    private String resolveDestinationAddress(Ride ride) {
        List<RideWaypoint> waypoints = rideWaypointRepository.findByRideOrderByWaypointOrderAsc(ride);
        return waypoints.isEmpty()
                ? "Unknown destination"
                : waypoints.get(waypoints.size() - 1).getLocation().getAddress();
    }
}
