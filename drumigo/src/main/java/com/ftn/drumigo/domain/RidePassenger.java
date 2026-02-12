package com.ftn.drumigo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "ride_passengers")
@IdClass(RidePassenger.RidePassengerId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RidePassenger {
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;
    
    @Id
    @Column(name = "passenger_email", nullable = false)
    private String passengerEmail;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RidePassengerId implements Serializable {
        private Long ride;
        private String passengerEmail;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RidePassengerId that = (RidePassengerId) o;
            return java.util.Objects.equals(ride, that.ride) &&
                   java.util.Objects.equals(passengerEmail, that.passengerEmail);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ride, passengerEmail);
        }
    }
}

