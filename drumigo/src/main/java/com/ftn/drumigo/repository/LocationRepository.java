package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    // Find location by address and coordinates (with tolerance for floating point precision)
    // For KT1, we'll match by address and approximate coordinates
    // In production, use a more sophisticated matching (e.g., distance-based)
    java.util.Optional<Location> findByAddressAndLatAndLng(String address, java.math.BigDecimal lat, java.math.BigDecimal lng);
}

