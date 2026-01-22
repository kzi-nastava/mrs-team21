package com.ftn.drumigo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passengers")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
public class Passenger extends User {
    
    public Passenger(Long id, String name, String surname, String email, String passwordHash, 
                     String address, String phone, String profilePictureUrl, Boolean blocked, 
                     Boolean active) {
        super(id, name, surname, email, passwordHash, address, phone, profilePictureUrl, 
              blocked, com.ftn.drumigo.domain.enums.UserRole.PASSENGER, active, null, null);
    }
}

