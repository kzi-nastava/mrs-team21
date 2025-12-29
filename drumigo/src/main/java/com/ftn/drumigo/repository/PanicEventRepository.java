package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.PanicEvent;
import com.ftn.drumigo.domain.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PanicEventRepository extends JpaRepository<PanicEvent, Long> {
    List<PanicEvent> findByRide(Ride ride);
}

