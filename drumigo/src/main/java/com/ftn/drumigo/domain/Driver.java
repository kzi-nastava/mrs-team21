package com.ftn.drumigo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "drivers")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
public class Driver extends User {
    
    @Column(name = "license_number")
    private String licenseNumber;
    
    @Column(name = "active_driver", nullable = false)
    private Boolean activeDriver = false;
    
    @Column(name = "last_state_change_at")
    private Instant lastStateChangeAt;
    
    public Driver(Long id, String name, String surname, String email, String passwordHash, 
                  String address, String phone, String profilePictureUrl, Boolean blocked, 
                  Boolean active, String licenseNumber, Boolean activeDriver, Instant lastStateChangeAt) {
        super(id, name, surname, email, passwordHash, address, phone, profilePictureUrl, 
              blocked, com.ftn.drumigo.domain.enums.UserRole.DRIVER, active, null, null);
        this.licenseNumber = licenseNumber;
        this.activeDriver = activeDriver;
        this.lastStateChangeAt = lastStateChangeAt;
    }
}

