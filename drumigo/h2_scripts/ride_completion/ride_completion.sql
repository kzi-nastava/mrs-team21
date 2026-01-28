-- Reusable test data for ride completion (IDs 9001/9002).
-- If your USERS table does NOT have a DTYPE column, remove that column from the MERGE statements below.

-- Clean any previous ride-related test data
DELETE FROM notifications WHERE ride_id = 9001 OR user_id IN (9001, 9002);
DELETE FROM reviews WHERE ride_id = 9001 OR passenger_id = 9002;
DELETE FROM ride_inconsistencies WHERE ride_id = 9001 OR passenger_id = 9002;
DELETE FROM panic_events WHERE ride_id = 9001 OR user_id IN (9001, 9002);
DELETE FROM ride_waypoints WHERE ride_id = 9001;
DELETE FROM ride_passengers WHERE ride_id = 9001;

-- Ensure a STANDARD vehicle type exists
MERGE INTO vehicle_types (name, start_price, price_per_km)
KEY (name)
VALUES ('STANDARD', 200.00, 50.00);

-- Driver user
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    -- Password: Test1234 (SHA-256 hashed, same as ride_rating script)
    9001, 'Test', 'Driver', 'driver9001@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Driver Address', '000-000', NULL, FALSE, 'DRIVER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Driver'
);

-- Passenger user
MERGE INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, role, active, created_at, updated_at, dtype
) KEY (id) VALUES (
    -- Password: Test1234 (SHA-256 hashed, same as ride_rating script)
    9002, 'Test', 'Passenger', 'passenger9002@test.local', '07480fb9e85b9396af06f006cf1c95024af2531c65fb505cfbd0add1e2f31573',
    'Passenger Address', '111-111', NULL, FALSE, 'PASSENGER', TRUE,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'Passenger'
);

-- Subclass rows
MERGE INTO drivers (user_id, license_number, active_driver, last_state_change_at)
KEY (user_id)
VALUES (9001, 'LIC-9001', TRUE, CURRENT_TIMESTAMP());

MERGE INTO passengers (user_id)
KEY (user_id)
VALUES (9002);

-- Vehicle linked to driver
MERGE INTO vehicles (
    id, driver_id, vehicle_type_id, model, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng, available
) KEY (id) VALUES (
    9001, 9001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'Test Car', 'TEST-9001', 4,
    FALSE, FALSE, NULL, NULL, FALSE
);

-- Locations
MERGE INTO locations (id, address, lat, lng)
KEY (id)
VALUES (9001, 'Test Pickup Address', 45.2600, 19.8300);

MERGE INTO locations (id, address, lat, lng)
KEY (id)
VALUES (9002, 'Test Destination Address', 45.2500, 19.8500);

-- Active ride
MERGE INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) KEY (id) VALUES (
    9001, 'ACTIVE', CURRENT_TIMESTAMP(), NULL, CURRENT_TIMESTAMP(), NULL, NULL,
    9001, 9001, 9002,
    500.00, 200.00, 50.00, 'STANDARD',
    5.50, 900, NULL,
    FALSE, FALSE, NULL, NULL,
    NULL, NULL
);

-- Waypoints
INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order)
VALUES
    (9001, 9001, 9001, 1),
    (9002, 9001, 9002, 2);

-- Passenger link
MERGE INTO ride_passengers (ride_id, passenger_id)
KEY (ride_id, passenger_id)
VALUES (9001, 9002);