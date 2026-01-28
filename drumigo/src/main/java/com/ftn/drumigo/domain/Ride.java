package com.ftn.drumigo.domain;

import com.ftn.drumigo.domain.enums.CancelReasonType;
import com.ftn.drumigo.domain.enums.RideStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;
    
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();
    
    @Column(name = "scheduled_for")
    private Instant scheduledFor;
    
    @Column(name = "start_time")
    private Instant startTime;
    
    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "paid_at")
    private Instant paidAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_passenger_id")
    private Passenger orderingPassenger;
    
    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;
    
    @Column(name = "pricing_start_price", precision = 10, scale = 2)
    private BigDecimal pricingStartPrice;
    
    @Column(name = "pricing_price_per_km", precision = 10, scale = 2)
    private BigDecimal pricingPricePerKm;
    
    @Column(name = "pricing_vehicle_type_name", length = 50)
    private String pricingVehicleTypeName;
    
    @Column(name = "total_distance_km", precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;
    
    @Column(name = "estimated_duration_sec")
    private Integer estimatedDurationSec;
    
    @Column(name = "estimated_arrival_at")
    private Instant estimatedArrivalAt;
    
    @Column(name = "baby_transport", nullable = false)
    private Boolean babyTransport = false;
    
    @Column(name = "pet_transport", nullable = false)
    private Boolean petTransport = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason_type")
    private CancelReasonType cancelReasonType;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by_user_id")
    private User canceledByUser;
    
    @Column(name = "stopped_at")
    private Instant stoppedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_location_id")
    private Location stopLocation;
}

