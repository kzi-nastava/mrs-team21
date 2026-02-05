package com.ftn.drumigo.dto.profile.response;

import com.ftn.drumigo.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String address;
    private String phone;
    private String profilePictureUrl;
    private Boolean blocked;
    private UserRole role;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Driver-specific fields
    private Boolean activeDriver;
    private Boolean isBusy;
    private Instant lastStateChangeAt;
    private VehicleInfoResponse vehicle;
}
