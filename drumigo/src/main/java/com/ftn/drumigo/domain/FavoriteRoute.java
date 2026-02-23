package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.users.Passenger;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "favorite_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRoute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;
    
    @Column(name = "baby_transport", nullable = false)
    private Boolean babyTransport = false;
    
    @Column(name = "pet_transport", nullable = false)
    private Boolean petTransport = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * When set, this favorite was created from a past ride (ride history).
     * Used to show isFavorite in history and to remove-by-ride.
     */
    @Column(name = "source_ride_id")
    private Long sourceRideId;
}

