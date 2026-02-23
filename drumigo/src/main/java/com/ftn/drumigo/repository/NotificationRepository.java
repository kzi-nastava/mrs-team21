package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Notification;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.enums.NotificationType;
import com.ftn.drumigo.domain.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.ride = :ride AND n.type = :type AND n.reminderMinutes = :reminderMinutesBefore")
    boolean existsByRideAndTypeAndReminderMinutesBefore(
        @Param("ride") Ride ride,
        @Param("type") NotificationType type,
        @Param("reminderMinutesBefore") Integer reminderMinutesBefore
    );
}

