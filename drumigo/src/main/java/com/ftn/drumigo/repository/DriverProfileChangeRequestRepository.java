package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.DriverProfileChangeRequest;
import com.ftn.drumigo.domain.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverProfileChangeRequestRepository extends JpaRepository<DriverProfileChangeRequest, Long> {
    Page<DriverProfileChangeRequest> findByStatus(RequestStatus status, Pageable pageable);
}

