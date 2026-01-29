package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.users.Passenger;
import com.ftn.drumigo.domain.users.User;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RidePassengerId implements Serializable {
        private Long ride;
        private Long passenger;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RidePassengerId that = (RidePassengerId) o;
            return java.util.Objects.equals(ride, that.ride) &&
                   java.util.Objects.equals(passenger, that.passenger);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ride, passenger);
        }
    }
}

