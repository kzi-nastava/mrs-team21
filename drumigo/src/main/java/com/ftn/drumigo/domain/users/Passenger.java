package com.ftn.drumigo.domain.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passengers")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
public class Passenger extends User {
}
