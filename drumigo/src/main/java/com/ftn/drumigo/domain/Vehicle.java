package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.users.Driver;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;
    
    @Column(name = "model", length = 100)
    private String model;
    
    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;
    
    @Column(name = "num_seats", nullable = false)
    private Integer numSeats;
    
    @Column(name = "baby_friendly", nullable = false)
    private Boolean babyFriendly = false;
    
    @Column(name = "pet_friendly", nullable = false)
    private Boolean petFriendly = false;
    
    @Column(name = "current_lat", precision = 10, scale = 8)
    private BigDecimal currentLat;
    
    @Column(name = "current_lng", precision = 11, scale = 8)
    private BigDecimal currentLng;
}

