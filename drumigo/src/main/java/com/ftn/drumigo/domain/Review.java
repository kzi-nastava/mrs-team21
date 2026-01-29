package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.users.Driver;
import com.ftn.drumigo.domain.users.Passenger;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ride_id", "passenger_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;
    
    @Column(name = "rating_driver", nullable = false)
    private Integer ratingDriver;
    
    @Column(name = "rating_vehicle", nullable = false)
    private Integer ratingVehicle;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

