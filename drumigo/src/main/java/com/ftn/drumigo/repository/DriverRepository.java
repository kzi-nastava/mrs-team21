package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByActiveDriverTrue();

    Optional<Driver> findByEmail(String email);
}

