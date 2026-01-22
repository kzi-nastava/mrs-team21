package com.ftn.drumigo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favorite_route_waypoints",
       uniqueConstraints = @UniqueConstraint(columnNames = {"favorite_route_id", "waypoint_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRouteWaypoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_route_id", nullable = false)
    private FavoriteRoute favoriteRoute;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;
    
    @Column(name = "waypoint_order", nullable = false)
    private Integer waypointOrder;
}

