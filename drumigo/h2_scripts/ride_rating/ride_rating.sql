-- Reusable test data for ride rating scenarios.
-- Creates various ride states for testing the rating feature:
-- 1. Ride with existing review (rated)
-- 2. Ride eligible for rating (FINISHED, within 3 days)
-- 3. Ride with expired rating deadline (FINISHED, past 3 days)
-- 4. Ride at deadline boundary (exactly 3 days)
-- 5. Cancelled ride (cannot be rated)
-- 6. Active ride (cannot be rated yet)

-- IDs used: 8001-8010 for users, vehicles, rides

-- ============================================
-- CLEANUP: Remove previous rating test data
-- ============================================
DELETE FROM notifications WHERE ride_id IN (8001, 8002, 8003, 8004, 8005, 8006) OR user_id IN (8001, 8002, 8003);
DELETE FROM reviews WHERE ride_id IN (8001, 8002, 8003, 8004, 8005, 8006);
DELETE FROM ride_inconsistencies WHERE ride_id IN (8001, 8002, 8003, 8004, 8005, 8006);
DELETE FROM panic_events WHERE ride_id IN (8001, 8002, 8003, 8004, 8005, 8006);
DELETE FROM ride_waypoints WHERE ride_id IN (8001, 8002, 8003, 8004, 8005, 8006);
DELETE FROM ride_passengers WHERE ride_id IN (8001, 8002, 8003, 8004, 8005, 8006);
DELETE FROM rides WHERE id IN (8001, 8002, 8003, 8004, 8005, 8006);
DELETE FROM vehicles WHERE id = 8001;
DELETE FROM locations WHERE id IN (8001, 8002, 8003, 8004, 8005, 8006, 8007, 8008, 8009, 8010, 8011, 8012);
DELETE FROM drivers WHERE user_id = 8001;
DELETE FROM passengers WHERE user_id IN (8002, 8003);
DELETE FROM users WHERE id IN (8001, 8002, 8003);

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
    profile_picture_url, blocked, role, created_at, updated_at, dtype
) KEY (id) VALUES (
    8001, 'Rating', 'TestDriver', 'rating.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Test Driver Address', '+381 64 111 0001', NULL, FALSE, 'DRIVER',
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);

-- Passenger 1 (main test passenger, password: Test1234)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, created_at, updated_at, dtype
) KEY (id) VALUES (
    8002, 'Rating', 'TestPassenger', 'rating.passenger@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Test Passenger Address', '+381 64 222 0002', NULL, FALSE, 'PASSENGER',
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Passenger 2 (linked passenger for some rides, password: Test1234)
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, created_at, updated_at, dtype
) KEY (id) VALUES (
    8003, 'Linked', 'Passenger', 'linked.passenger@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Linked Passenger Address', '+381 64 333 0003', NULL, FALSE, 'PASSENGER',
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Driver subclass
MERGE INTO drivers (user_id, active_driver, last_state_change_at)
KEY (user_id)
VALUES (8001, TRUE, CURRENT_TIMESTAMP());

-- Passenger subclasses
MERGE INTO passengers (user_id) KEY (user_id) VALUES (8002);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (8003);

-- ============================================
-- VEHICLE
-- ============================================
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) KEY (id) VALUES (
    8001, 8001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'RT-8001', 4,
    FALSE, FALSE, 45.2600, 19.8300
);

-- ============================================
-- LOCATIONS
-- ============================================
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES 
    (8001, 'Bulevar Oslobodjenja 1, Novi Sad', 45.2550, 19.8350),
    (8002, 'Trg Slobode 1, Novi Sad', 45.2540, 19.8420),
    (8003, 'Petrovaradin, Novi Sad', 45.2510, 19.8650),
    (8004, 'Liman, Novi Sad', 45.2420, 19.8280),
    (8005, 'Detelinara, Novi Sad', 45.2680, 19.8150),
    (8006, 'Telep, Novi Sad', 45.2380, 19.8220),
    (8007, 'Podbara, Novi Sad', 45.2620, 19.8480),
    (8008, 'Grbavica, Novi Sad', 45.2480, 19.8380),
    (8009, 'Sajmiste, Novi Sad', 45.2300, 19.8100),
    (8010, 'Novo Naselje, Novi Sad', 45.2580, 19.7980),
    (8011, 'Klisa, Novi Sad', 45.2700, 19.7850),
    (8012, 'Adamovicevo Naselje, Novi Sad', 45.2350, 19.8550);

-- ============================================
-- SCENARIO 1: Ride with existing review (rated)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    8001, 'FINISHED', 
    DATEADD('DAY', -2, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -2, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -2, DATEADD('MINUTE', 25, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -2, DATEADD('MINUTE', 25, CURRENT_TIMESTAMP())),
    8001, 8001, 8002,
    450.00, 200.00, 50.00, 'STANDARD',
    5.00, 1500, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8001, 8001, 8001, 1),
    (8002, 8001, 8002, 2);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (8001, 8002);

-- Existing review for this ride
INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (8001, 8002, 5, 4, 'Great driver, very professional! Vehicle was clean.', DATEADD('DAY', -1, CURRENT_TIMESTAMP()));

-- ============================================
-- SCENARIO 2: Ride eligible for rating (FINISHED, within 3 days)
-- Ended 1 day ago - CAN RATE (2 days left)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    8002, 'FINISHED', 
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -1, DATEADD('MINUTE', 18, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -1, DATEADD('MINUTE', 18, CURRENT_TIMESTAMP())),
    8001, 8001, 8002,
    380.00, 200.00, 50.00, 'STANDARD',
    3.60, 1080, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8003, 8002, 8003, 1),
    (8004, 8002, 8004, 2);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (8002, 8002);

-- ============================================
-- SCENARIO 3: Ride with expired rating deadline
-- Ended 5 days ago - CANNOT RATE (deadline passed)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    8003, 'FINISHED', 
    DATEADD('DAY', -5, CURRENT_TIMESTAMP()), 
    NULL, 
    DATEADD('DAY', -5, CURRENT_TIMESTAMP()), 
    DATEADD('DAY', -5, DATEADD('MINUTE', 30, CURRENT_TIMESTAMP())), 
    DATEADD('DAY', -5, DATEADD('MINUTE', 30, CURRENT_TIMESTAMP())),
    8001, 8001, 8002,
    520.00, 200.00, 50.00, 'STANDARD',
    6.40, 1800, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8005, 8003, 8005, 1),
    (8006, 8003, 8006, 2);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (8003, 8002);

-- ============================================
-- SCENARIO 4: Ride at deadline boundary
-- Ended exactly 3 days ago minus 1 hour - CAN RATE (barely)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    8004, 'FINISHED', 
    DATEADD('HOUR', -71, CURRENT_TIMESTAMP()),  -- 2 days 23 hours ago
    NULL, 
    DATEADD('HOUR', -71, CURRENT_TIMESTAMP()), 
    DATEADD('HOUR', -71, DATEADD('MINUTE', 22, CURRENT_TIMESTAMP())), 
    DATEADD('HOUR', -71, DATEADD('MINUTE', 22, CURRENT_TIMESTAMP())),
    8001, 8001, 8002,
    410.00, 200.00, 50.00, 'STANDARD',
    4.20, 1320, NULL,
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8007, 8004, 8007, 1),
    (8008, 8004, 8008, 2);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (8004, 8002);

-- ============================================
-- SCENARIO 5: Cancelled ride (cannot be rated)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    8005, 'CANCELLED', 
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 
    NULL, 
    NULL, 
    NULL, 
    NULL,
    8001, 8001, 8002,
    0.00, 200.00, 50.00, 'STANDARD',
    NULL, NULL, NULL,
    FALSE, FALSE, 'Passenger was not at pickup location', 8001,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8009, 8005, 8009, 1),
    (8010, 8005, 8010, 2);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (8005, 8002);

-- ============================================
-- SCENARIO 6: Active ride (cannot be rated yet)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    8006, 'ACTIVE', 
    CURRENT_TIMESTAMP(), 
    NULL, 
    CURRENT_TIMESTAMP(), 
    NULL, 
    NULL,
    8001, 8001, 8002,
    480.00, 200.00, 50.00, 'STANDARD',
    5.60, 1440, DATEADD('MINUTE', 24, CURRENT_TIMESTAMP()),
    FALSE, FALSE, NULL, NULL,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8011, 8006, 8011, 1),
    (8012, 8006, 8012, 2);

INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (8006, 8002);

-- ============================================
-- SUMMARY: Test scenarios created
-- ============================================
-- Ride 8001: FINISHED with existing review (rated) - CANNOT rate again
-- Ride 8002: FINISHED 1 day ago - CAN rate (2 days left)
-- Ride 8003: FINISHED 5 days ago - CANNOT rate (deadline passed)
-- Ride 8004: FINISHED ~3 days ago - CAN rate (barely within deadline)
-- Ride 8005: CANCELLED - CANNOT rate
-- Ride 8006: ACTIVE - CANNOT rate (not finished yet)

-- Test user credentials:
-- Email: rating.passenger@test.local
-- Password: Test1234 (SHA-256 hashed)
