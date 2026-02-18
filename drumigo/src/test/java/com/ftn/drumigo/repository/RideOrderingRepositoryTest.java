package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Vehicle;
import com.ftn.drumigo.domain.VehicleType;
import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.enums.VehicleTypeName;
import com.ftn.drumigo.domain.users.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RideOrderingRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private VehicleTypeRepository vehicleTypeRepository;
    @Autowired
    private LocationRepository locationRepository;

    @Test
    void vehicleTypeRepository_findByName_returnsVehicleType() {
        Optional<VehicleType> standard = vehicleTypeRepository.findByName(VehicleTypeName.STANDARD);

        assertTrue(standard.isPresent());
        assertEquals(VehicleTypeName.STANDARD, standard.orElseThrow().getName());
    }

    @Test
    void locationRepository_findByAddressAndLatAndLng_returnsExactMatch() {
        Location location = new Location();
        location.setAddress("Test pickup point");
        location.setLat(BigDecimal.valueOf(45.254321));
        location.setLng(BigDecimal.valueOf(19.842345));
        location = locationRepository.saveAndFlush(location);

        Optional<Location> found = locationRepository.findByAddressAndLatAndLng(
            "Test pickup point",
            BigDecimal.valueOf(45.254321),
            BigDecimal.valueOf(19.842345)
        );

        assertTrue(found.isPresent());
        assertEquals(location.getId(), found.orElseThrow().getId());
    }

    @Test
    void vehicleRepository_findByDriver_returnsDriversVehicle() {
        VehicleType standardType = vehicleTypeRepository.findByName(VehicleTypeName.STANDARD).orElseThrow();
        Driver driver = persistDriver("vehicle.find.driver@mail.com", true);
        Vehicle vehicle = persistVehicle(driver, standardType, "NS-100-TS");

        Optional<Vehicle> found = vehicleRepository.findByDriver(driver);

        assertTrue(found.isPresent());
        assertEquals(vehicle.getId(), found.orElseThrow().getId());
        assertEquals("NS-100-TS", found.orElseThrow().getLicensePlate());
    }

    @Test
    void vehicleRepository_existsByLicensePlate_checksPresence() {
        VehicleType standardType = vehicleTypeRepository.findByName(VehicleTypeName.STANDARD).orElseThrow();
        Driver driver = persistDriver("vehicle.exists.plate@mail.com", true);
        persistVehicle(driver, standardType, "NS-200-TS");

        assertTrue(vehicleRepository.existsByLicensePlate("NS-200-TS"));
        assertFalse(vehicleRepository.existsByLicensePlate("NS-404-TS"));
    }

    @Test
    void vehicleRepository_findByDriverActiveDriverTrue_returnsOnlyVehiclesOfActiveDrivers() {
        VehicleType standardType = vehicleTypeRepository.findByName(VehicleTypeName.STANDARD).orElseThrow();
        Driver activeDriver = persistDriver("vehicle.active.driver@mail.com", true);
        Driver inactiveDriver = persistDriver("vehicle.inactive.driver@mail.com", false);
        Vehicle activeVehicle = persistVehicle(activeDriver, standardType, "NS-300-TS");
        Vehicle inactiveVehicle = persistVehicle(inactiveDriver, standardType, "NS-301-TS");

        List<Vehicle> activeDriverVehicles = vehicleRepository.findByDriverActiveDriverTrue();
        Set<Long> vehicleIds = activeDriverVehicles.stream().map(Vehicle::getId).collect(Collectors.toSet());

        assertTrue(vehicleIds.contains(activeVehicle.getId()));
        assertFalse(vehicleIds.contains(inactiveVehicle.getId()));
    }

    private Driver persistDriver(String email, boolean activeDriver) {
        Driver driver = new Driver();
        driver.setName("Driver");
        driver.setSurname("Ordering");
        driver.setEmail(email);
        driver.setActive(true);
        driver.setRole(UserRole.DRIVER);
        driver.setBlocked(false);
        driver.setActiveDriver(activeDriver);
        driver.setBusy(false);
        return driverRepository.saveAndFlush(driver);
    }

    private Vehicle persistVehicle(Driver driver, VehicleType type, String licensePlate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setModel("Model X");
        vehicle.setLicensePlate(licensePlate);
        vehicle.setVehicleType(type);
        vehicle.setNumSeats(4);
        vehicle.setBabyFriendly(true);
        vehicle.setPetFriendly(true);
        vehicle.setCurrentLat(BigDecimal.valueOf(45.25));
        vehicle.setCurrentLng(BigDecimal.valueOf(19.84));
        return vehicleRepository.saveAndFlush(vehicle);
    }
}