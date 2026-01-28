-- Test data for ride tracking inconsistency feature (spec 2.6.2).
-- Creates an active ride with passengers for testing inconsistency reporting:
-- 1. ACTIVE ride with ordering passenger and linked passengers
-- 2. Existing inconsistency reports for verification
-- 3. Passenger who is NOT part of the ride (for authorization testing)

-- IDs used: 6001-6020 for users, vehicles, rides, locations

-- ============================================
-- CLEANUP: Remove previous test data
-- ============================================
DELETE FROM notifications WHERE ride_id = 6001 OR user_id IN (6001, 6002, 6003, 6004, 6005);
DELETE FROM reviews WHERE ride_id = 6001;
DELETE FROM ride_inconsistencies WHERE ride_id = 6001;
DELETE FROM panic_events WHERE ride_id = 6001;
DELETE FROM ride_waypoints WHERE ride_id = 6001;
DELETE FROM ride_passengers WHERE ride_id = 6001;
DELETE FROM rides WHERE id = 6001;
DELETE FROM vehicles WHERE id = 6001;
DELETE FROM locations WHERE id BETWEEN 6001 AND 6010;
DELETE FROM drivers WHERE user_id = 6001;
DELETE FROM passengers WHERE user_id IN (6002, 6003, 6004, 6005);
DELETE FROM users WHERE id IN (6001, 6002, 6003, 6004, 6005);

-- ============================================
-- SETUP: Ensure vehicle type exists
-- ============================================
MERGE INTO vehicle_types (name, start_price, price_per_km)
KEY (name)
VALUES ('STANDARD', 200.00, 50.00);

-- ============================================
-- USERS: Driver and Passengers
-- ============================================

-- Driver user (password: Test1234)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    6001, 'Tracking', 'TestDriver', 'tracking.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Driver Test Address 1', '+381 64 600 0001', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);

-- Passenger 1: Ordering passenger (password: Test1234)
-- Use this to test reporting inconsistencies as the main passenger
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    6002, 'Main', 'Passenger', 'main.passenger@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 1', '+381 64 600 0002', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 2: Linked passenger (password: Test1234)
-- Use this to test reporting inconsistencies as a linked passenger
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    6003, 'Linked', 'Passenger', 'linked.passenger@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 2', '+381 64 600 0003', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 3: Another linked passenger (password: Test1234)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    6004, 'Another', 'LinkedPassenger', 'another.linked@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 3', '+381 64 600 0004', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 4: NOT part of the ride (password: Test1234)
-- Use this to test authorization - should get 400 error when trying to report
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    6005, 'Unauthorized', 'Passenger', 'unauthorized.passenger@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 4', '+381 64 600 0005', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Driver subclass
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id)
VALUES (6001, 'LIC-TRACKING-6001', TRUE, CURRENT_TIMESTAMP());

-- Passenger subclasses
MERGE INTO passengers (user_id) KEY (user_id) VALUES (6002);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (6003);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (6004);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (6005);

-- ============================================
-- VEHICLE
-- ============================================
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    6001, 6001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Toyota Corolla', 'NS-6001-AA', 4,
    TRUE, FALSE, 45.2550, 19.8350, FALSE
);

-- ============================================
-- LOCATIONS
-- ============================================
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES 
    (6001, 'Bulevar Oslobodjenja 50, Novi Sad', 45.2550, 19.8350),
    (6002, 'Trg Slobode 1, Novi Sad', 45.2540, 19.8420),
    (6003, 'Futoska 10, Novi Sad', 45.2480, 19.8280);

-- ============================================
-- ACTIVE RIDE: For testing inconsistency reporting
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    6001, 'ACTIVE', 
    DATEADD('MINUTE', -10, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('MINUTE', -5, CURRENT_TIMESTAMP()), 
    NULL, 
    NULL,
    6001, 6001, 6002,
    450.00, 200.00, 50.00, 'STANDARD',
    5.00, 900, DATEADD('MINUTE', 10, CURRENT_TIMESTAMP()),
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

-- Ride waypoints (start and destination)
INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (6001, 6001, 6001, 0),
    (6002, 6001, 6002, 1);

-- Link passengers to the ride
-- Passenger 6002 is the ordering passenger (already linked via ordering_passenger_id)
-- Passengers 6003 and 6004 are linked passengers
INSERT INTO ride_passengers (ride_id, passenger_id) VALUES 
    (6001, 6002),
    (6001, 6003),
    (6001, 6004);

-- ============================================
-- EXISTING INCONSISTENCY REPORTS: For verification
-- ============================================
-- Inconsistency report from ordering passenger
INSERT INTO ride_inconsistencies (id, ride_id, passenger_id, note, created_at) VALUES
    (6001, 6001, 6002, 'Driver took a longer route through Liman instead of going directly to Trg Slobode.', DATEADD('MINUTE', -3, CURRENT_TIMESTAMP()));

-- Inconsistency report from linked passenger
INSERT INTO ride_inconsistencies (id, ride_id, passenger_id, note, created_at) VALUES
    (6002, 6001, 6003, 'I noticed the driver missed the turn at Futoska and had to go around the block.', DATEADD('MINUTE', -1, CURRENT_TIMESTAMP()));

-- ============================================
-- SUMMARY: Test scenarios created
-- ============================================
-- Ride 6001: ACTIVE ride for testing
--   - Ordering passenger: 6002 (main.passenger@test.local)
--   - Linked passengers: 6003 (linked.passenger@test.local), 6004 (another.linked@test.local)
--   - NOT in ride: 6005 (unauthorized.passenger@test.local)
--   - Has 2 existing inconsistency reports
--
-- Test credentials (all use password: Test1234):
--   - main.passenger@test.local - Can report (ordering passenger)
--   - linked.passenger@test.local - Can report (linked passenger)
--   - another.linked@test.local - Can report (linked passenger)
--   - unauthorized.passenger@test.local - CANNOT report (not in ride)
--   - tracking.driver@test.local - Driver (cannot report, different role)
