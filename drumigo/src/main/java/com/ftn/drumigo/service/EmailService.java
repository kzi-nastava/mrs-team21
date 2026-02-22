package com.ftn.drumigo.service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /** Optional. If set (e.g. drumigo://activate-driver), driver activation email uses this so the link opens the mobile app. */
    @Value("${app.driver-activation.link:}")
    private String driverActivationLink;

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender mailSender;

    private boolean sendEmail(String email, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            if (sender != null && !sender.isBlank()) {
                message.setFrom(sender);
            }

            mailSender.send(message);
            log.info("Email successfully sent to {} with subject '{}'", email, subject);
            return true;

        } catch (Exception e) {
            // Email delivery must never break core business flows. Persisted domain data remains intact.
            log.warn("Failed to send email to {}: {} - check SMTP config and spam folder", email, e.getMessage());
            return false;
        }
    }

    @Async
    public void sendActivationEmail(String email, String token) {
        String activationLink = frontendUrl + "/activate/" + token;
        String subject = "Activate Your Drumigo Account";
        String body = String.format(
                "Hello,\n\n" +
                        "Thank you for registering!\n\n" +
                        "Please click the link below to activate your account:\n" +
                        "%s\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "Best regards,\n" +
                        "Team 9+10",
                activationLink
        );
        sendEmail(email, subject, body);
    }

    @Async
    public void sendDriverActivationEmail(String email, String token) {
        log.info("Sending driver activation email to {} (link base configured: {})", email, driverActivationLink != null && !driverActivationLink.isBlank());
        String activationLink;
        if (driverActivationLink != null && !driverActivationLink.isBlank()) {
            String base = driverActivationLink.trim();
            // Strip /api so link points to backend root: /activate-driver (redirect controller), not /api/activate-driver
            if (base.endsWith("/api")) {
                base = base.substring(0, base.length() - 4);
            } else if (base.endsWith("/api/")) {
                base = base.substring(0, base.length() - 5);
            }
            if (base.contains("/activate-driver")) {
                activationLink = base.endsWith("/") ? base + token : base + "/" + token;
            } else {
                String noTrailing = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
                activationLink = noTrailing + "/activate-driver/" + token;
            }
        } else {
            activationLink = frontendUrl + "/activate-driver/" + token;
        }
        String subject = "Set Your Drumigo Driver Password";
        String body = String.format(
                "Hello,\n\n" +
                        "Your driver account has been created by an administrator.\n\n" +
                        "Open the link below and set your password:\n" +
                        "%s\n\n" +
                        "If your app does not have this page yet, use API endpoint:\n" +
                        "PUT /api/activation/{token}/set-password\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "Best regards,\n" +
                        "Team 9+10",
                activationLink
        );
        // Fire-and-forget: admin should not wait on SMTP latency.
        sendEmail(email, subject, body);
    }

    @Async
    public void sendRideFinishedEmail(
            String email,
            Long rideId,
            String pickupAddress,
            String destinationAddress,
            boolean canRate
    ) {
        String rideHistoryLink = frontendUrl + "/ride-history?rideId=" + rideId;
        String subject = "Your Drumigo ride has finished";
        String ratingNote = canRate
                ? "You can rate the driver and vehicle in the app."
                : "If you ordered this ride, you can rate the driver and vehicle in the app.";

        String body = String.format(
                "Hello,\n\n" +
                        "Your ride has finished.\n\n" +
                        "From: %s\n" +
                        "To: %s\n\n" +
                        "%s\n" +
                        "Open Ride History: %s\n\n" +
                        "Best regards,\n" +
                        "Team 9+10",
                pickupAddress,
                destinationAddress,
                ratingNote,
                rideHistoryLink
        );
        sendEmail(email, subject, body);
    }

    @Async
    public void sendLinkedPassengerRideAcceptedEmail(
            String email,
            Long rideId,
            String pickupAddress,
            String destinationAddress
    ) {
        String trackingLink = frontendUrl + "/ride-tracking/" + rideId;
        String subject = "You've been added to a Drumigo ride";
        String body = String.format(
                "Hello,\n\n" +
                        "You have been added to a ride and it has been accepted.\n\n" +
                        "From: %s\n" +
                        "To: %s\n\n" +
                        "Track your ride: %s\n\n" +
                        "Best regards,\n" +
                        "Team 9+10",
                pickupAddress,
                destinationAddress,
                trackingLink
        );
        sendEmail(email, subject, body);
    }

    @Async
    public void sendLinkedPassengerRideRejectedEmail(String email, String reason) {
        String subject = "Ride request was not accepted";
        String body = String.format(
                "Hello,\n\n" +
                        "The ride you were linked to could not be fulfilled.\n\n" +
                        "Reason: %s\n\n" +
                        "Please try again later or contact support if you have questions.\n\n" +
                        "Best regards,\n" +
                        "Team 9+10",
                reason
        );
        sendEmail(email, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = frontendUrl + "/reset-password/" + token;
        String subject = "Reset Your Drumigo Password";
        String body = String.format(
                "Hello,\n\n" +
                        "We received a request to reset your Drumigo password.\n\n" +
                        "Please click the link below to reset your password:\n" +
                        "%s\n\n" +
                        "This link will expire in 30 minutes.\n\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Team 9+10",
                resetLink
        );
        sendEmail(email, subject, body);
    }
}