-- Sample rides for H2: adds a few rides in different statuses for testing.
-- Run after the app has started (schema exists). Use in H2 Console with:
--   jdbc:h2:mem:drumigo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
--
-- Rides created:
--   9501: PENDING  (just requested, no driver yet)
--   9502: ACCEPTED (driver accepted, not started)
--   9503: ACTIVE   (in progress)
--   9504: FINISHED (completed with times and paid_at)
--
-- IDs used: 9501-9508 (users, driver, passengers, vehicle, locations, rides, waypoints)

-- ============================================
-- CLEANUP: Remove previous sample ride data
-- ============================================
DELETE FROM notifications WHERE ride_id IN (9501, 9502, 9503, 9504) OR user_id IN (9501, 9502, 9503);
DELETE FROM reviews WHERE ride_id IN (9501, 9502, 9503, 9504);
DELETE FROM ride_inconsistencies WHERE ride_id IN (9501, 9502, 9503, 9504);
DELETE FROM panic_events WHERE ride_id IN (9501, 9502, 9503, 9504);
DELETE FROM ride_waypoints WHERE ride_id IN (9501, 9502, 9503, 9504);
DELETE FROM ride_passengers WHERE ride_id IN (9501, 9502, 9503, 9504);
DELETE FROM rides WHERE id IN (9501, 9502, 9503, 9504);
DELETE FROM vehicles WHERE id = 9501;
DELETE FROM locations WHERE id BETWEEN 9501 AND 9508;
DELETE FROM drivers WHERE user_id = 9501;
DELETE FROM passengers WHERE user_id IN (9502, 9503);
DELETE FROM users WHERE id IN (9501, 9502, 9503);

-- ============================================
-- SETUP: Vehicle type
-- ============================================
MERGE INTO vehicle_types (name, start_price, price_per_km)
KEY (name)
VALUES ('STANDARD', 200.00, 50.00);

-- ============================================
-- USERS: Driver and Passengers
-- ============================================
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    9501, 'Sample', 'Driver', 'sample.driver@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Sample Driver Address', '+381 64 950 0001', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);

MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    9502, 'Sample', 'Passenger1', 'sample.passenger1@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Sample Passenger Address 1', '+381 64 950 0002', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    9503, 'Sample', 'Passenger2', 'sample.passenger2@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Sample Passenger Address 2', '+381 64 950 0003', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id)
VALUES (9501, 'LIC-SAMPLE-9501', TRUE, CURRENT_TIMESTAMP());

MERGE INTO passengers (user_id) KEY (user_id) VALUES (9502);
MERGE INTO passengers (user_id) KEY (user_id) VALUES (9503);

-- ============================================
-- VEHICLE
-- ============================================
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    9501, 9501, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Sample Car', 'SMP-9501', 4,
    FALSE, FALSE, 45.26, 19.84, TRUE
);

-- ============================================
-- LOCATIONS (pickup/destination per ride)
-- ============================================
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9501, 'Pickup Ride 1', 45.2600, 19.8300);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9502, 'Destination Ride 1', 45.2500, 19.8500);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9503, 'Pickup Ride 2', 45.2550, 19.8350);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9504, 'Destination Ride 2', 45.2450, 19.8450);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9505, 'Pickup Ride 3', 45.2620, 19.8280);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9506, 'Destination Ride 3', 45.2520, 19.8520);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9507, 'Pickup Ride 4', 45.2580, 19.8380);
MERGE INTO locations (id, address, lat, lng) KEY (id) VALUES (9508, 'Destination Ride 4', 45.2480, 19.8480);

-- ============================================
-- RIDE 9501: PENDING (no driver/vehicle yet)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    9501, 'PENDING', CURRENT_TIMESTAMP(), NULL, NULL, NULL, NULL,
    NULL, NULL, 9502,
    350.00, 200.00, 50.00, 'STANDARD',
    3.00, 600, NULL,
    FALSE, FALSE, NULL, NULL,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (9501, 9501, 9501, 1),
    (9502, 9501, 9502, 2);
INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (9501, 9502);

-- ============================================
-- RIDE 9502: ACCEPTED (driver accepted, not started)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    9502, 'ACCEPTED', DATEADD('MINUTE', -15, CURRENT_TIMESTAMP()), NULL, NULL, NULL, NULL,
    9501, 9501, 9502,
    420.00, 200.00, 50.00, 'STANDARD',
    4.40, 780, NULL,
    FALSE, FALSE, NULL, NULL,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (9503, 9502, 9503, 1),
    (9504, 9502, 9504, 2);
INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (9502, 9502);

-- ============================================
-- RIDE 9503: ACTIVE (in progress)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    9503, 'ACTIVE', DATEADD('MINUTE', -20, CURRENT_TIMESTAMP()), NULL, DATEADD('MINUTE', -5, CURRENT_TIMESTAMP()), NULL, NULL,
    9501, 9501, 9502,
    480.00, 200.00, 50.00, 'STANDARD',
    5.60, 900, DATEADD('MINUTE', 10, CURRENT_TIMESTAMP()),
    FALSE, FALSE, NULL, NULL,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (9505, 9503, 9505, 1),
    (9506, 9503, 9506, 2);
INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (9503, 9502), (9503, 9503);

-- ============================================
-- RIDE 9504: FINISHED (completed, paid)
-- ============================================
INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    9504, 'FINISHED',
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), NULL,
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()), DATEADD('DAY', -1, DATEADD('MINUTE', 22, CURRENT_TIMESTAMP())), DATEADD('DAY', -1, DATEADD('MINUTE', 22, CURRENT_TIMESTAMP())),
    9501, 9501, 9502,
    520.00, 200.00, 50.00, 'STANDARD',
    6.40, 1320, NULL,
    FALSE, FALSE, NULL, NULL,
    NULL, 9508
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (9507, 9504, 9507, 1),
    (9508, 9504, 9508, 2);
INSERT INTO ride_passengers (ride_id, passenger_id) VALUES (9504, 9502);
