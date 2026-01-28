package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    java.util.Optional<Vehicle> findByDriver(com.ftn.drumigo.domain.users.Driver driver);

    List<Vehicle> findByAvailableTrue();
    boolean existsByLicensePlate(String licensePlate);
    
    /**
     * Find all vehicles belonging to active drivers (drivers currently working).
     * Returns both available and busy vehicles for display on the landing page map.
     */
    @Query("SELECT v FROM Vehicle v WHERE v.driver.activeDriver = true")
    List<Vehicle> findByDriverActiveDriverTrue();

}

