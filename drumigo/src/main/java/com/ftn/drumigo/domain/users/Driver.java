package com.ftn.drumigo.domain.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class Driver extends User {
    
    @Column(name = "active_driver", nullable = false)
    private Boolean activeDriver = false;
    
    @Column(name = "last_state_change_at")
    private Instant lastStateChangeAt;
}
