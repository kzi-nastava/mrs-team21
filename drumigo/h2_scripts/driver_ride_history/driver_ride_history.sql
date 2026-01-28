-- Reusable test data for driver ride history feature (spec 2.9.2).
-- Creates a driver with various ride scenarios for testing:
-- 1. FINISHED ride with review (multiple passengers)
-- 2. FINISHED ride without review
-- 3. CANCELLED ride (by driver)
-- 4. CANCELLED ride (by passenger)
-- 5. FINISHED ride with PANIC event
-- 6. FINISHED rides at various dates (for filter testing)
-- 7. ACTIVE ride (should NOT appear in history)

-- IDs used: 7001-7015 for users, vehicles, rides, locations

-- ============================================
-- CLEANUP: Remove previous driver history test data
-- ============================================
DELETE FROM notifications WHERE ride_id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008) OR user_id IN (7001, 7002, 7003, 7004, 7005);
DELETE FROM reviews WHERE ride_id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008);
DELETE FROM ride_inconsistencies WHERE ride_id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008);
DELETE FROM panic_events WHERE ride_id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008);
DELETE FROM ride_waypoints WHERE ride_id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008);
DELETE FROM ride_passengers WHERE ride_id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008);
DELETE FROM rides WHERE id IN (7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008);
DELETE FROM vehicles WHERE id = 7001;
DELETE FROM locations WHERE id BETWEEN 7001 AND 7020;
DELETE FROM drivers WHERE user_id = 7001;
DELETE FROM passengers WHERE user_id IN (7002, 7003, 7004, 7005);
DELETE FROM users WHERE id IN (7001, 7002, 7003, 7004, 7005);

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
    7001, 'History', 'TestDriver', 'history.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Driver Test Address 1', '+381 64 700 0001', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);

-- Passenger 1 (ordering passenger, password: Test1234)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    7002, 'Marko', 'Petrovic', 'marko.petrovic@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 1', '+381 64 700 0002', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 2 (linked passenger)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    7003, 'Ana', 'Jovanovic', 'ana.jovanovic@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 2', '+381 64 700 0003', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 3 (linked passenger)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    7004, 'Nikola', 'Stojanovic', 'nikola.stojanovic@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 3', '+381 64 700 0004', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 4 (for cancelled by passenger scenario)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    7005, 'Jovana', 'Nikolic', 'jovana.nikolic@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Test Address 4', '+381 64 700 0005', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Driver subclass
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id)
VALUES (7001, 'LIC-HISTORY-7001', TRUE, CURRENT_TIMESTAMP());

-- Passenger subclasses
MERGE INTO passengers (user_id) KEY (user_id) VALUES (7002);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (7003);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (7004);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (7005);

-- ============================================
-- VEHICLE
-- ============================================
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    7001, 7001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'VW Golf 8', 'NS-7001-AA', 4,
    TRUE, FALSE, 45.2550, 19.8350, TRUE
);

-- ============================================
-- LOCATIONS
-- ============================================
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES 
    (7001, 'Bulevar Oslobodjenja 50, Novi Sad', 45.2550, 19.8350),
    (7002, 'Trg Slobode 1, Novi Sad', 45.2540, 19.8420),
    (7003, 'Futoska 10, Novi Sad', 45.2480, 19.8280),
    (7004, 'Liman 4, Novi Sad', 45.2420, 19.8260),
    (7005, 'Petrovaradin Fortress, Novi Sad', 45.2515, 19.8625),
    (7006, 'Strand Beach, Novi Sad', 45.2680, 19.8580),
    (7007, 'Railway Station, Novi Sad', 45.2670, 19.8330),
    (7008, 'Spens, Novi Sad', 45.2530, 19.8150),
    (7009, 'Detelinara, Novi Sad', 45.2700, 19.8100),
    (7010, 'Telep, Novi Sad', 45.2380, 19.8220),
    (7011, 'Grbavica, Novi Sad', 45.2450, 19.8400),
    (7012, 'Rotkvarija, Novi Sad', 45.2600, 19.8450),
    (7013, 'Klisa, Novi Sad', 45.2720, 19.7900),
    (7014, 'Sajmiste, Novi Sad', 45.2350, 19.8100),
    (7015, 'Novo Naselje, Novi Sad', 45.2580, 19.7980),
    (7016, 'Podbara, Novi Sad', 45.2620, 19.8480);

-- ============================================
-- SCENARIO 1: FINISHED ride with review (multiple passengers) - 2 days ago
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7001, 'FINISHED', 
    DATEADD('DAY', -2, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -2, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -2, DATEADD('MINUTE', 20, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -2, DATEADD('MINUTE', 20, CURRENT_TIMESTAMP())),
    7001, 7001, 7002,
    560.00, 200.00, 50.00, 'STANDARD',
    7.20, 1200, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7001, 7001, 7001, 0),
    (7002, 7001, 7002, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES 
    (7001, 7002),
    (7001, 7003),
    (7001, 7004);

-- Review for this ride
INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (7001, 7002, 5, 5, 'Excellent ride! Driver was very professional.', DATEADD('DAY', -2, CURRENT_TIMESTAMP()));

-- ============================================
-- SCENARIO 2: FINISHED ride without review - 1 day ago
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7002, 'FINISHED', 
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -1, DATEADD('MINUTE', 15, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -1, DATEADD('MINUTE', 15, CURRENT_TIMESTAMP())),
    7001, 7001, 7003,
    380.00, 200.00, 50.00, 'STANDARD',
    3.60, 900, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7003, 7002, 7003, 0),
    (7004, 7002, 7004, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7002, 7003);

-- Review for this ride (ordering passenger = 7003)
INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (7002, 7003, 4, 4, 'Smooth ride, arrived on time. Vehicle was clean.', DATEADD('DAY', -1, DATEADD('MINUTE', 30, CURRENT_TIMESTAMP())));

-- ============================================
-- SCENARIO 3: CANCELLED by driver - 3 days ago
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7003, 'CANCELLED', 
    DATEADD('DAY', -3, CURRENT_TIMESTAMP()), 
    NULL, 
    NULL, 
    NULL, 
    NULL,
    7001, 7001, 7004,
    0.00, 200.00, 50.00, 'STANDARD',
    NULL, NULL, NULL,
    FALSE, FALSE, 'Passenger not at pickup location after 5 minutes wait', 7001,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7005, 7003, 7005, 0),
    (7006, 7003, 7006, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7003, 7004);

-- ============================================
-- SCENARIO 4: CANCELLED by passenger - 4 days ago
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7004, 'CANCELLED', 
    DATEADD('DAY', -4, CURRENT_TIMESTAMP()), 
    NULL, 
    NULL, 
    NULL, 
    NULL,
    7001, 7001, 7005,
    0.00, 200.00, 50.00, 'STANDARD',
    NULL, NULL, NULL,
    FALSE, FALSE, NULL, 7005,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7007, 7004, 7007, 0),
    (7008, 7004, 7008, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7004, 7005);

-- ============================================
-- SCENARIO 5: FINISHED ride with PANIC event - 5 days ago
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7005, 'FINISHED', 
    DATEADD('DAY', -5, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -5, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -5, DATEADD('MINUTE', 25, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -5, DATEADD('MINUTE', 25, CURRENT_TIMESTAMP())),
    7001, 7001, 7002,
    620.00, 200.00, 50.00, 'STANDARD',
    8.40, 1500, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7009, 7005, 7009, 0),
    (7010, 7005, 7010, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7005, 7002);

-- PANIC event for this ride
INSERT INTO panic_events (ride_id, user_id, reason, created_at) VALUES
    (7005, 7002, 'Driver was driving too fast on a busy road', DATEADD('DAY', -5, DATEADD('MINUTE', 10, CURRENT_TIMESTAMP())));

-- Review for this ride (ordering passenger = 7002)
INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (7005, 7002, 2, 3, 'I felt unsafe during parts of the ride. Please improve driving style.', DATEADD('DAY', -5, DATEADD('MINUTE', 40, CURRENT_TIMESTAMP())));

-- ============================================
-- SCENARIO 6: FINISHED ride - 10 days ago (for date filter testing)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7006, 'FINISHED', 
    DATEADD('DAY', -10, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -10, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -10, DATEADD('MINUTE', 12, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -10, DATEADD('MINUTE', 12, CURRENT_TIMESTAMP())),
    7001, 7001, 7003,
    320.00, 200.00, 50.00, 'STANDARD',
    2.40, 720, NULL,
    TRUE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7011, 7006, 7011, 0),
    (7012, 7006, 7012, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7006, 7003);

-- Review for this ride (ordering passenger = 7003)
INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (7006, 7003, 5, 4, 'Great driver, friendly and careful. Vehicle was fine.', DATEADD('DAY', -10, DATEADD('MINUTE', 25, CURRENT_TIMESTAMP())));

-- ============================================
-- SCENARIO 7: FINISHED ride - 20 days ago (for date filter testing)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7007, 'FINISHED', 
    DATEADD('DAY', -20, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -20, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -20, DATEADD('MINUTE', 30, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -20, DATEADD('MINUTE', 30, CURRENT_TIMESTAMP())),
    7001, 7001, 7004,
    750.00, 200.00, 50.00, 'STANDARD',
    11.00, 1800, NULL,
    FALSE, TRUE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7013, 7007, 7013, 0),
    (7014, 7007, 7014, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7007, 7004);

-- ============================================
-- SCENARIO 8: ACTIVE ride (should NOT appear in history)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    7008, 'ACTIVE', 
    CURRENT_TIMESTAMP(), 
    NULL, 
    CURRENT_TIMESTAMP(), 
    NULL, 
    NULL,
    7001, 7001, 7002,
    420.00, 200.00, 50.00, 'STANDARD',
    4.40, 1100, DATEADD('MINUTE', 18, CURRENT_TIMESTAMP()),
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7015, 7008, 7015, 0),
    (7016, 7008, 7016, 1);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (7008, 7002);

-- ============================================
-- SUMMARY: Test scenarios created
-- ============================================
-- Ride 7001: FINISHED 2 days ago, 3 passengers, has review
-- Ride 7002: FINISHED 1 day ago, 1 passenger, no review
-- Ride 7003: CANCELLED by driver 3 days ago
-- Ride 7004: CANCELLED by passenger 4 days ago
-- Ride 7005: FINISHED 5 days ago with PANIC event
-- Ride 7006: FINISHED 10 days ago (date filter test)
-- Ride 7007: FINISHED 20 days ago (date filter test)
-- Ride 7008: ACTIVE (should NOT appear in history)

-- Test driver credentials:
-- Email: history.driver@test.local
-- Password: Test1234 (SHA-256 hashed)
