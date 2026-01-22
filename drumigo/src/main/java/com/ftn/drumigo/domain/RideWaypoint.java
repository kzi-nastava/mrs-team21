package com.ftn.drumigo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ride_waypoints",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ride_id", "waypoint_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideWaypoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;
    
    @Column(name = "waypoint_order", nullable = false)
    private Integer waypointOrder;
}

