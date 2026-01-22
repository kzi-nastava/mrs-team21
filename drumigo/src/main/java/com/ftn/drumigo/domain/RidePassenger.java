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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RidePassengerId implements Serializable {
        private Long ride;
        private Long passenger;
    }
}

