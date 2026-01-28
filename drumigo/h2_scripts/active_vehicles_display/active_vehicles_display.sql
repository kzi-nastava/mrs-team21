-- Test data for active vehicles display feature (spec 2.1.1).
-- Creates 10 drivers with vehicles in various states:
-- - 8 active drivers (activeDriver=true) - should appear on map
--   - 4 available vehicles (free - green markers)
--   - 4 busy vehicles (on ride - red markers)
-- - 2 inactive drivers (activeDriver=false) - should NOT appear on map

-- IDs used: 8001-8012 for users, 8001-8010 for vehicles

-- ============================================
-- CLEANUP: Remove previous active vehicles test data
-- ============================================
DELETE FROM vehicles WHERE id BETWEEN 8001 AND 8010;
DELETE FROM drivers WHERE user_id BETWEEN 8001 AND 8012;
DELETE FROM users WHERE id BETWEEN 8001 AND 8012;

-- ============================================
-- SETUP: Ensure vehicle types exist
-- ============================================
MERGE INTO vehicle_types (name, start_price, price_per_km)
KEY (name)
VALUES ('STANDARD', 200.00, 50.00);

MERGE INTO vehicle_types (name, start_price, price_per_km)
KEY (name)
VALUES ('LUXURY', 400.00, 80.00);

MERGE INTO vehicle_types (name, start_price, price_per_km)
KEY (name)
VALUES ('VAN', 300.00, 60.00);

-- ============================================
-- ACTIVE DRIVERS WITH AVAILABLE VEHICLES (Free - Green markers)
-- These should appear on the map with green markers
-- ============================================

-- Driver 1: Active, Available (City Center)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8001, 'Marko', 'Petrovic', 'marko.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Bulevar Oslobodjenja 50', '+381 64 800 0001', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8001, 'LIC-8001', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8001, 8001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Toyota Corolla', 'NS-800-AA', 4, FALSE, TRUE, 45.2671, 19.8335, TRUE
);

-- Driver 2: Active, Available (Liman area)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8002, 'Ana', 'Jovanovic', 'ana.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Liman 4', '+381 64 800 0002', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8002, 'LIC-8002', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8002, 8002, (SELECT id FROM vehicle_types WHERE name = 'LUXURY'),
    'Mercedes-Benz E-Class', 'NS-800-BB', 4, TRUE, TRUE, 45.2556, 19.8447, TRUE
);

-- Driver 3: Active, Available (Detelinara area)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8003, 'Milica', 'Stojanovic', 'milica.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Detelinara 15', '+381 64 800 0003', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8003, 'LIC-8003', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8003, 8003, (SELECT id FROM vehicle_types WHERE name = 'VAN'),
    'Ford Transit', 'NS-800-CC', 8, TRUE, TRUE, 45.2805, 19.8203, TRUE
);

-- Driver 4: Active, Available (Telep area)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8004, 'Luka', 'Djordjevic', 'luka.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Telep 22', '+381 64 800 0004', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8004, 'LIC-8004', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8004, 8004, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Renault Clio', 'NS-800-DD', 4, FALSE, FALSE, 45.2598, 19.8124, TRUE
);

-- ============================================
-- ACTIVE DRIVERS WITH BUSY VEHICLES (On ride - Red markers)
-- These should appear on the map with red markers
-- ============================================

-- Driver 5: Active, Busy (Petrovaradin)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8005, 'Stefan', 'Nikolic', 'stefan.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Petrovaradin', '+381 64 800 0005', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8005, 'LIC-8005', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8005, 8005, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Volkswagen Golf', 'NS-800-EE', 4, FALSE, FALSE, 45.2517, 19.8369, FALSE
);

-- Driver 6: Active, Busy (Grbavica area)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8006, 'Sara', 'Popovic', 'sara.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Grbavica', '+381 64 800 0006', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8006, 'LIC-8006', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8006, 8006, (SELECT id FROM vehicle_types WHERE name = 'LUXURY'),
    'BMW 5 Series', 'NS-800-FF', 4, TRUE, FALSE, 45.2734, 19.8578, FALSE
);

-- Driver 7: Active, Busy (Near Danube)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8007, 'Nikola', 'Radovic', 'nikola.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Kej, Novi Sad', '+381 64 800 0007', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8007, 'LIC-8007', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8007, 8007, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Opel Astra', 'NS-800-GG', 4, FALSE, TRUE, 45.2487, 19.8291, FALSE
);

-- Driver 8: Active, Busy (Rotkvarija area)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8008, 'Jovana', 'Ilic', 'jovana.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Rotkvarija', '+381 64 800 0008', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8008, 'LIC-8008', TRUE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8008, 8008, (SELECT id FROM vehicle_types WHERE name = 'VAN'),
    'Mercedes Sprinter', 'NS-800-HH', 8, TRUE, TRUE, 45.2645, 19.8489, FALSE
);

-- ============================================
-- INACTIVE DRIVERS (activeDriver=false)
-- These should NOT appear on the map
-- ============================================

-- Driver 9: INACTIVE, Available vehicle (Sremska Kamenica)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8009, 'Jovan', 'Markovic', 'jovan.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Sremska Kamenica', '+381 64 800 0009', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8009, 'LIC-8009', FALSE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8009, 8009, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Peugeot 308', 'NS-800-II', 4, FALSE, TRUE, 45.2432, 19.8015, TRUE
);

-- Driver 10: INACTIVE, Available vehicle (City center area)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    8010, 'Marija', 'Tomic', 'marija.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Centar', '+381 64 800 0010', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id) VALUES (8010, 'LIC-8010', FALSE, CURRENT_TIMESTAMP());
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    8010, 8010, (SELECT id FROM vehicle_types WHERE name = 'LUXURY'),
    'Audi A6', 'NS-800-JJ', 4, TRUE, TRUE, 45.2712, 19.8415, TRUE
);

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
-- Run these to verify the data is correct:

-- Should return 8 vehicles (from active drivers)
-- SELECT v.id, v.model, v.license_plate, v.available, d.active_driver 
-- FROM vehicles v 
-- JOIN drivers d ON v.driver_id = d.user_id 
-- WHERE d.active_driver = TRUE AND v.id BETWEEN 8001 AND 8010;

-- Should return 0 vehicles (from inactive drivers - these shouldn't appear on map)
-- SELECT v.id, v.model, v.license_plate, v.available, d.active_driver 
-- FROM vehicles v 
-- JOIN drivers d ON v.driver_id = d.user_id 
-- WHERE d.active_driver = FALSE AND v.id BETWEEN 8001 AND 8010;
