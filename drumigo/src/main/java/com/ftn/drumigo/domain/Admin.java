package com.ftn.drumigo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends User {
    
    public Admin(Long id, String name, String surname, String email, String passwordHash, 
                 String address, String phone, String profilePictureUrl, Boolean blocked, 
                 Boolean active) {
        super(id, name, surname, email, passwordHash, address, phone, profilePictureUrl, 
              blocked, com.ftn.drumigo.domain.enums.UserRole.ADMIN, active, null, null);
    }
}

