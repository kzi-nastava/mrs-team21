package com.ftn.drumigo.service;

import com.ftn.drumigo.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender mailSender;

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

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            // Use configured SMTP username as the From address (Gmail requires this)
            if (sender != null && !sender.isBlank()) {
                message.setFrom(sender);
            }

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send activation email to {}: {}", email, e.getMessage(), e);
            throw new EmailSendException("Failed to send activation email", e);
        }
    }
}