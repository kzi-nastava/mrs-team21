package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:location_repo_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.test.database.replace=NONE",
    "maintenance.basic.username=admin",
    "maintenance.basic.password=admin",
    "jwt.secret=testSecretKeyThatIsAtLeast32CharactersLongForHS256",
    "jwt.expiration=3600000"
})
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void shouldFindLocationByAddressAndCoordinates_whenExactMatchExists() {
        Location location = new Location();
        location.setAddress("Bulevar Oslobodjenja 1");
        location.setLat(new BigDecimal("45.25100000"));
        location.setLng(new BigDecimal("19.84500000"));
        locationRepository.save(location);

        Optional<Location> found = locationRepository.findByAddressAndLatAndLng(
            "Bulevar Oslobodjenja 1",
            new BigDecimal("45.25100000"),
            new BigDecimal("19.84500000")
        );

        assertTrue(found.isPresent());
        assertEquals("Bulevar Oslobodjenja 1", found.get().getAddress());
    }

    @Test
    void shouldReturnEmpty_whenCoordinatesDiffer() {
        Location location = new Location();
        location.setAddress("Bulevar Oslobodjenja 1");
        location.setLat(new BigDecimal("45.25100000"));
        location.setLng(new BigDecimal("19.84500000"));
        locationRepository.save(location);

        Optional<Location> found = locationRepository.findByAddressAndLatAndLng(
            "Bulevar Oslobodjenja 1",
            new BigDecimal("45.25200000"),
            new BigDecimal("19.84500000")
        );

        assertTrue(found.isEmpty());
    }
}
