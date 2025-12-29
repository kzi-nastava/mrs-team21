package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Driver;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByStatus(RideStatus status);
    List<Ride> findByDriverAndStatus(Driver driver, RideStatus status);
    
    @Query("SELECT r FROM Ride r WHERE r.driver = :driver AND r.requestedAt BETWEEN :from AND :to")
    Page<Ride> findByDriverAndRequestedAtBetween(@Param("driver") Driver driver, 
                                                   @Param("from") Instant from, 
                                                   @Param("to") Instant to, 
                                                   Pageable pageable);
    
    @Query("SELECT r FROM Ride r WHERE r.driver.name LIKE %:name% OR r.driver.surname LIKE %:name%")
    List<Ride> findByDriverNameContaining(@Param("name") String name);
}

