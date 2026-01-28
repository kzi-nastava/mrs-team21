package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    Optional<Object> findByEmail(String email);
}

