package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.users.Driver;
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
    Page<Ride> findByDriverAndStatusAndScheduledForAfter(Driver driver, RideStatus status, Instant scheduledFor, Pageable pageable);
    
    @Query("SELECT r FROM Ride r WHERE r.driver = :driver AND r.requestedAt BETWEEN :from AND :to")
    Page<Ride> findByDriverAndRequestedAtBetween(@Param("driver") Driver driver, 
                                                   @Param("from") Instant from, 
                                                   @Param("to") Instant to, 
                                                   Pageable pageable);
    
    @Query("SELECT r FROM Ride r WHERE r.driver.name LIKE CONCAT('%', :name, '%') OR r.driver.surname LIKE CONCAT('%', :name, '%')")
    List<Ride> findByDriverNameContaining(@Param("name") String name);
    
    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.requestedAt >= :from AND r.requestedAt <= :to")
    List<Ride> findByStatusAndRequestedAtBetween(@Param("status") RideStatus status,
                                                   @Param("from") Instant from,
                                                   @Param("to") Instant to);
    
    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.requestedAt >= :from AND r.requestedAt <= :to " +
           "AND (r.orderingPassenger.id = :userId OR EXISTS " +
           "(SELECT rp FROM RidePassenger rp WHERE rp.ride = r AND rp.passengerEmail = :passengerEmail))")
    List<Ride> findByStatusAndUserAndRequestedAtBetween(@Param("status") RideStatus status,
                                                          @Param("userId") Long userId,
                                                          @Param("passengerEmail") String passengerEmail,
                                                          @Param("from") Instant from,
                                                          @Param("to") Instant to);

    @Query("""
           SELECT DISTINCT r
           FROM Ride r
           WHERE r.requestedAt >= :from AND r.requestedAt <= :to
             AND (r.orderingPassenger.id = :passengerId OR EXISTS
                 (SELECT rp FROM RidePassenger rp WHERE rp.ride = r AND rp.passengerEmail = :passengerEmail))
             AND (:statuses IS NULL OR r.status IN :statuses)
             AND (:hasPanic IS NULL OR
                  (:hasPanic = TRUE AND EXISTS (SELECT pe FROM PanicEvent pe WHERE pe.ride = r)) OR
                  (:hasPanic = FALSE AND NOT EXISTS (SELECT pe FROM PanicEvent pe WHERE pe.ride = r))
             )
           """)
    Page<Ride> findPassengerHistory(@Param("passengerId") Long passengerId,
                                   @Param("passengerEmail") String passengerEmail,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   @Param("statuses") List<RideStatus> statuses,
                                   @Param("hasPanic") Boolean hasPanic,
                                   Pageable pageable);

    @Query("""
           SELECT DISTINCT r
           FROM Ride r
           WHERE r.requestedAt >= :from AND r.requestedAt <= :to
             AND (:statuses IS NULL OR r.status IN :statuses)
             AND (:hasPanic IS NULL OR
                  (:hasPanic = TRUE AND EXISTS (SELECT pe FROM PanicEvent pe WHERE pe.ride = r)) OR
                  (:hasPanic = FALSE AND NOT EXISTS (SELECT pe FROM PanicEvent pe WHERE pe.ride = r))
             )
           """)
    Page<Ride> findAdminRideHistory(@Param("from") Instant from,
                                    @Param("to") Instant to,
                                    @Param("statuses") List<RideStatus> statuses,
                                    @Param("hasPanic") Boolean hasPanic,
                                    Pageable pageable);
}
