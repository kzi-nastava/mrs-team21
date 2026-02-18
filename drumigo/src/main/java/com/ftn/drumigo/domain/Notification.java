package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private Ride ride;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * For SCHEDULED_RIDE_REMINDER: minutes before ride start (15, 10, or 5).
     * Null for other notification types.
     */
    @Column(name = "reminder_minutes_before")
    private Integer reminderMinutes;

    /** Explicit setter so IDEs and reflection reliably see it (column: reminder_minutes_before). */
    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }
}

