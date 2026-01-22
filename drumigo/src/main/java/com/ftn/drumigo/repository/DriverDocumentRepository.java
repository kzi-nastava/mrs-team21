package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Driver;
import com.ftn.drumigo.domain.DriverDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {
    List<DriverDocument> findByDriver(Driver driver);
}

