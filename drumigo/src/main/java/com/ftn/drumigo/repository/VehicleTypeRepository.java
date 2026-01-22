package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.domain.enums.VehicleTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    Optional<VehicleType> findByName(VehicleTypeName name);
}

