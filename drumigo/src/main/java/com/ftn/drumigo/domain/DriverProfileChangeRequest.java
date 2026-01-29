package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.enums.RequestStatus;
import com.ftn.drumigo.domain.users.Admin;
import com.ftn.drumigo.domain.users.Driver;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "driver_profile_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileChangeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    
    @Column(name = "requested_changes_json", nullable = false, columnDefinition = "TEXT")
    private String requestedChangesJson;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private Admin reviewedByAdmin;
}

