package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    java.util.Optional<Vehicle> findByDriver(com.ftn.drumigo.domain.users.Driver driver);
}

