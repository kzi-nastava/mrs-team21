-- Combined MySQL seed data (converted from H2 scripts)
-- Assumes schema already exists (code-first via Hibernate).

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- VEHICLE TYPES
-- =====================================================================
INSERT INTO vehicle_types (name, start_price, price_per_km)
VALUES
    ('STANDARD', 200.00, 50.00),
    ('LUXURY', 400.00, 80.00),
    ('VAN', 300.00, 60.00)
ON DUPLICATE KEY UPDATE
    start_price = VALUES(start_price),
    price_per_km = VALUES(price_per_km);

-- =====================================================================
-- PROFILE TEST DATA
-- =====================================================================
INSERT INTO users (dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, active, role, created_at, updated_at)
VALUES
    ('Passenger', 'Ana', 'Petrović', 'ana.petrovic@example.com', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Bulevar Oslobođenja 10, Novi Sad', '+381641234567', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW()),
    ('Driver', 'Marko', 'Jovanović', 'marko.jovanovic@example.com', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Narodnih Heroja 25, Beograd', '+381652345678', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW()),
    ('Admin', 'Stefan', 'Nikolić', 'stefan.nikolic@drumigo.com', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Trg Republike 1, Beograd', '+381663456789', NULL, FALSE, TRUE, 'ADMIN', NOW(), NOW()),
    ('Driver', 'Jelena', 'Milošević', 'jelena.milosevic@example.com', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Knez Mihailova 30, Beograd', '+381641112222', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW()),
    ('Driver', 'Nikola', 'Đorđević', 'nikola.djordjevic@example.com', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Cara Dušana 15, Niš', '+381652223333', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW()),
    ('Passenger', 'Milica', 'Stojanović', 'milica.stojanovic@example.com', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Kraljevića Marka 5, Kragujevac', '+381663334444', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    dtype = VALUES(dtype),
    name = VALUES(name),
    surname = VALUES(surname),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

INSERT INTO passengers (user_id)
VALUES
    ((SELECT id FROM users WHERE email = 'ana.petrovic@example.com')),
    ((SELECT id FROM users WHERE email = 'milica.stojanovic@example.com'))
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES
    ((SELECT id FROM users WHERE email = 'marko.jovanovic@example.com'), TRUE, FALSE, NOW()),
    ((SELECT id FROM users WHERE email = 'jelena.milosevic@example.com'), FALSE, FALSE, NOW()),
    ((SELECT id FROM users WHERE email = 'nikola.djordjevic@example.com'), TRUE, FALSE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO admins (user_id)
VALUES ((SELECT id FROM users WHERE email = 'stefan.nikolic@drumigo.com'))
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO vehicles (driver_id, license_plate, vehicle_type_id, num_seats, baby_friendly, pet_friendly, current_lat, current_lng)
VALUES
    ((SELECT id FROM users WHERE email = 'marko.jovanovic@example.com'), 'NS-001-AB',
     (SELECT id FROM vehicle_types WHERE name = 'STANDARD'), 4, TRUE, FALSE, 45.2671, 19.8335),
    ((SELECT id FROM users WHERE email = 'jelena.milosevic@example.com'), 'BG-123-CD',
     (SELECT id FROM vehicle_types WHERE name = 'LUXURY'), 4, FALSE, TRUE, 44.7866, 20.4489),
    ((SELECT id FROM users WHERE email = 'nikola.djordjevic@example.com'), 'NI-456-EF',
     (SELECT id FROM vehicle_types WHERE name = 'VAN'), 7, TRUE, TRUE, 43.3209, 21.8954)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

-- =====================================================================
-- RIDE TRACKING INCONSISTENCY (IDs 6001-6005)
-- =====================================================================
INSERT INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, active, role, created_at, updated_at, dtype
) VALUES
    (6001, 'Tracking', 'TestDriver', 'tracking.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Driver Test Address 1', '+381 64 600 0001', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (6002, 'Main', 'Passenger', 'main.passenger@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 1', '+381 64 600 0002', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (6003, 'Linked', 'Passenger', 'linked.passenger@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 2', '+381 64 600 0003', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (6004, 'Another', 'LinkedPassenger', 'another.linked@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 3', '+381 64 600 0004', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (6005, 'Unauthorized', 'Passenger', 'unauthorized.passenger@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 4', '+381 64 600 0005', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    surname = VALUES(surname),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    dtype = VALUES(dtype);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES (6001, TRUE, TRUE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO passengers (user_id) VALUES (6002), (6003), (6004), (6005)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) VALUES (
    6001, 6001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'NS-6001-AA', 4, TRUE, FALSE, 45.2550, 19.8350
)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    license_plate = VALUES(license_plate),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

INSERT INTO locations (id, address, lat, lng) VALUES
    (6001, 'Bulevar Oslobodjenja 50, Novi Sad', 45.2550, 19.8350),
    (6002, 'Trg Slobode 1, Novi Sad', 45.2540, 19.8420),
    (6003, 'Futoska 10, Novi Sad', 45.2480, 19.8280)
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    lat = VALUES(lat),
    lng = VALUES(lng);

INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    6001, 'ACTIVE',
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    NULL,
    DATE_SUB(NOW(), INTERVAL 5 MINUTE),
    NULL,
    NULL,
    6001, 6001, 6002,
    450.00, 200.00, 50.00, 'STANDARD',
    5.00, 900, DATE_ADD(NOW(), INTERVAL 10 MINUTE),
    FALSE, FALSE, NULL, NULL, NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (6001, 6001, 6001, 0),
    (6002, 6001, 6002, 1);

INSERT INTO ride_passengers (ride_id, passenger_email) VALUES
    (6001, 'linked.passenger@test.local'),
    (6001, 'another.linked@test.local'),
    (6001, 'unauthorized.passenger@test.local');

INSERT INTO ride_inconsistencies (id, ride_id, passenger_id, note, created_at) VALUES
    (6001, 6001, 6002, 'Driver took a longer route through Liman instead of going directly to Trg Slobode.', DATE_SUB(NOW(), INTERVAL 3 MINUTE)),
    (6002, 6001, 6003, 'I noticed the driver missed the turn at Futoska and had to go around the block.', DATE_SUB(NOW(), INTERVAL 1 MINUTE));

-- =====================================================================
-- DRIVER RIDE HISTORY (IDs 7001-7016)
-- =====================================================================
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

INSERT INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, active, role, created_at, updated_at, dtype
) VALUES
    (7001, 'History', 'TestDriver', 'history.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Driver Test Address 1', '+381 64 700 0001', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (7002, 'Marko', 'Petrovic', 'marko.petrovic@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 1', '+381 64 700 0002', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (7003, 'Ana', 'Jovanovic', 'ana.jovanovic@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 2', '+381 64 700 0003', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (7004, 'Nikola', 'Stojanovic', 'nikola.stojanovic@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 3', '+381 64 700 0004', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (7005, 'Jovana', 'Nikolic', 'jovana.nikolic@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Test Address 4', '+381 64 700 0005', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    surname = VALUES(surname),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    dtype = VALUES(dtype);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES (7001, TRUE, TRUE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO passengers (user_id) VALUES (7002), (7003), (7004), (7005)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) VALUES (
    7001, 7001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'NS-7001-AA', 4, TRUE, FALSE, 45.2550, 19.8350
)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    license_plate = VALUES(license_plate),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

INSERT INTO locations (id, address, lat, lng) VALUES
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
    (7016, 'Podbara, Novi Sad', 45.2620, 19.8480)
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    lat = VALUES(lat),
    lng = VALUES(lng);

INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES
    (7001, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 2 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 2 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 20 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 20 MINUTE),
     7001, 7001, 7002,
     560.00, 200.00, 50.00, 'STANDARD',
     7.20, 1200, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (7002, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 15 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 15 MINUTE),
     7001, 7001, 7003,
     380.00, 200.00, 50.00, 'STANDARD',
     3.60, 900, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (7003, 'CANCELLED',
     DATE_SUB(NOW(), INTERVAL 3 DAY),
     NULL,
     NULL, NULL, NULL,
     7001, 7001, 7004,
     0.00, 200.00, 50.00, 'STANDARD',
     NULL, NULL, NULL,
     FALSE, FALSE, 'Passenger not at pickup location after 5 minutes wait', 7001,
     NULL, NULL),
    (7004, 'CANCELLED',
     DATE_SUB(NOW(), INTERVAL 4 DAY),
     NULL,
     NULL, NULL, NULL,
     7001, 7001, 7005,
     0.00, 200.00, 50.00, 'STANDARD',
     NULL, NULL, NULL,
     FALSE, FALSE, NULL, 7005,
     NULL, NULL),
    (7005, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 5 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 5 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 25 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 25 MINUTE),
     7001, 7001, 7002,
     620.00, 200.00, 50.00, 'STANDARD',
     8.40, 1500, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (7006, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 10 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 10 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 12 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 12 MINUTE),
     7001, 7001, 7003,
     320.00, 200.00, 50.00, 'STANDARD',
     2.40, 720, NULL,
     TRUE, FALSE, NULL, NULL, NULL, NULL),
    (7007, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 20 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 20 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 30 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 30 MINUTE),
     7001, 7001, 7004,
     750.00, 200.00, 50.00, 'STANDARD',
     11.00, 1800, NULL,
     FALSE, TRUE, NULL, NULL, NULL, NULL),
    (7008, 'ACTIVE',
     NOW(),
     NULL,
     NOW(),
     NULL,
     NULL,
     7001, 7001, 7002,
     420.00, 200.00, 50.00, 'STANDARD',
     4.40, 1100, DATE_ADD(NOW(), INTERVAL 18 MINUTE),
     FALSE, FALSE, NULL, NULL, NULL, NULL);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (7001, 7001, 7001, 0),
    (7002, 7001, 7002, 1),
    (7003, 7002, 7003, 0),
    (7004, 7002, 7004, 1),
    (7005, 7003, 7005, 0),
    (7006, 7003, 7006, 1),
    (7007, 7004, 7007, 0),
    (7008, 7004, 7008, 1),
    (7009, 7005, 7009, 0),
    (7010, 7005, 7010, 1),
    (7011, 7006, 7011, 0),
    (7012, 7006, 7012, 1),
    (7013, 7007, 7013, 0),
    (7014, 7007, 7014, 1),
    (7015, 7008, 7015, 0),
    (7016, 7008, 7016, 1);

INSERT INTO ride_passengers (ride_id, passenger_email) VALUES
    (7001, 'marko.petrovic@test.local'),
    (7001, 'ana.jovanovic@test.local'),
    (7001, 'nikola.stojanovic@test.local'),
    (7002, 'ana.jovanovic@test.local'),
    (7003, 'nikola.stojanovic@test.local'),
    (7004, 'jovana.nikolic@test.local'),
    (7005, 'marko.petrovic@test.local'),
    (7006, 'ana.jovanovic@test.local'),
    (7007, 'nikola.stojanovic@test.local'),
    (7008, 'marko.petrovic@test.local');

INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (7001, 7002, 5, 5, 'Excellent ride! Driver was very professional.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (7002, 7003, 4, 4, 'Smooth ride, arrived on time. Vehicle was clean.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 30 MINUTE)),
    (7005, 7002, 2, 3, 'I felt unsafe during parts of the ride. Please improve driving style.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 40 MINUTE)),
    (7006, 7003, 5, 4, 'Great driver, friendly and careful. Vehicle was fine.', DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 25 MINUTE));

INSERT INTO panic_events (ride_id, user_id, reason, created_at) VALUES
    (7005, 7002, 'Driver was driving too fast on a busy road', DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 10 MINUTE));

-- =====================================================================
-- ACTIVE VEHICLES DISPLAY (IDs 8001-8010)
-- =====================================================================
DELETE FROM vehicles WHERE id BETWEEN 8001 AND 8010;
DELETE FROM drivers WHERE user_id BETWEEN 8001 AND 8012;
DELETE FROM users WHERE id BETWEEN 8001 AND 8012;

INSERT INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, active, role, created_at, updated_at, dtype
) VALUES
    (8001, 'Marko', 'Petrovic', 'marko.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Bulevar Oslobodjenja 50', '+381 64 800 0001', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8002, 'Ana', 'Jovanovic', 'ana.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Liman 4', '+381 64 800 0002', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8003, 'Milica', 'Stojanovic', 'milica.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Detelinara 15', '+381 64 800 0003', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8004, 'Luka', 'Djordjevic', 'luka.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Telep 22', '+381 64 800 0004', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8005, 'Stefan', 'Nikolic', 'stefan.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Petrovaradin', '+381 64 800 0005', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8006, 'Sara', 'Popovic', 'sara.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Grbavica', '+381 64 800 0006', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8007, 'Nikola', 'Radovic', 'nikola.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Kej, Novi Sad', '+381 64 800 0007', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8008, 'Jovana', 'Ilic', 'jovana.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Rotkvarija', '+381 64 800 0008', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8009, 'Jovan', 'Markovic', 'jovan.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Sremska Kamenica', '+381 64 800 0009', NULL, FALSE,  TRUE,'DRIVER', NOW(), NOW(), 'Driver'),
    (8010, 'Marija', 'Tomic', 'marija.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Centar', '+381 64 800 0010', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    surname = VALUES(surname),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    dtype = VALUES(dtype);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES
    (8001, TRUE, FALSE, NOW()),
    (8002, TRUE, FALSE, NOW()),
    (8003, TRUE, FALSE, NOW()),
    (8004, TRUE, FALSE, NOW()),
    (8005, TRUE, TRUE, NOW()),
    (8006, TRUE, TRUE, NOW()),
    (8007, TRUE, TRUE, NOW()),
    (8008, TRUE, TRUE, NOW()),
    (8009, FALSE, FALSE, NOW()),
    (8010, FALSE, FALSE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) VALUES
    (8001, 8001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'), 'NS-800-AA', 4, FALSE, TRUE, 45.2671, 19.8335),
    (8002, 8002, (SELECT id FROM vehicle_types WHERE name = 'LUXURY'), 'NS-800-BB', 4, TRUE, TRUE, 45.2556, 19.8447),
    (8003, 8003, (SELECT id FROM vehicle_types WHERE name = 'VAN'), 'NS-800-CC', 8, TRUE, TRUE, 45.2805, 19.8203),
    (8004, 8004, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'), 'NS-800-DD', 4, FALSE, FALSE, 45.2598, 19.8124),
    (8005, 8005, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'), 'NS-800-EE', 4, FALSE, FALSE, 45.2517, 19.8369),
    (8006, 8006, (SELECT id FROM vehicle_types WHERE name = 'LUXURY'), 'NS-800-FF', 4, TRUE, FALSE, 45.2734, 19.8578),
    (8007, 8007, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'), 'NS-800-GG', 4, FALSE, TRUE, 45.2487, 19.8291),
    (8008, 8008, (SELECT id FROM vehicle_types WHERE name = 'VAN'), 'NS-800-HH', 8, TRUE, TRUE, 45.2645, 19.8489),
    (8009, 8009, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'), 'NS-800-II', 4, FALSE, TRUE, 45.2432, 19.8015),
    (8010, 8010, (SELECT id FROM vehicle_types WHERE name = 'LUXURY'), 'NS-800-JJ', 4, TRUE, TRUE, 45.2712, 19.8415)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    license_plate = VALUES(license_plate),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

-- =====================================================================
-- RIDE RATING (IDs 8501-8512)
-- =====================================================================
DELETE FROM notifications WHERE ride_id IN (8501, 8502, 8503, 8504, 8505, 8506) OR user_id IN (8501, 8502, 8503);
DELETE FROM reviews WHERE ride_id IN (8501, 8502, 8503, 8504, 8505, 8506);
DELETE FROM ride_inconsistencies WHERE ride_id IN (8501, 8502, 8503, 8504, 8505, 8506);
DELETE FROM panic_events WHERE ride_id IN (8501, 8502, 8503, 8504, 8505, 8506);
DELETE FROM ride_waypoints WHERE ride_id IN (8501, 8502, 8503, 8504, 8505, 8506);
DELETE FROM ride_passengers WHERE ride_id IN (8501, 8502, 8503, 8504, 8505, 8506);
DELETE FROM rides WHERE id IN (8501, 8502, 8503, 8504, 8505, 8506);
DELETE FROM vehicles WHERE id = 8501;
DELETE FROM locations WHERE id BETWEEN 8501 AND 8512;
DELETE FROM drivers WHERE user_id = 8501;
DELETE FROM passengers WHERE user_id IN (8502, 8503);
DELETE FROM users WHERE id IN (8501, 8502, 8503);

INSERT INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, active, role, created_at, updated_at, dtype
) VALUES
    (8501, 'Rating', 'TestDriver', 'rating.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Test Driver Address', '+381 64 111 0001', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (8502, 'Rating', 'TestPassenger', 'rating.passenger@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Test Passenger Address', '+381 64 222 0002', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (8503, 'Linked', 'Passenger', 'linked.passenger@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Linked Passenger Address', '+381 64 333 0003', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    surname = VALUES(surname),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    dtype = VALUES(dtype);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES (8501, TRUE, TRUE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO passengers (user_id) VALUES (8502), (8503)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) VALUES (
    8501, 8501, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'RT-8501', 4, FALSE, FALSE, 45.2600, 19.8300
)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    license_plate = VALUES(license_plate),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

INSERT INTO locations (id, address, lat, lng) VALUES
    (8501, 'Bulevar Oslobodjenja 1, Novi Sad', 45.2550, 19.8350),
    (8502, 'Trg Slobode 1, Novi Sad', 45.2540, 19.8420),
    (8503, 'Petrovaradin, Novi Sad', 45.2510, 19.8650),
    (8504, 'Liman, Novi Sad', 45.2420, 19.8280),
    (8505, 'Detelinara, Novi Sad', 45.2680, 19.8150),
    (8506, 'Telep, Novi Sad', 45.2380, 19.8220),
    (8507, 'Podbara, Novi Sad', 45.2620, 19.8480),
    (8508, 'Grbavica, Novi Sad', 45.2480, 19.8380),
    (8509, 'Sajmiste, Novi Sad', 45.2300, 19.8100),
    (8510, 'Novo Naselje, Novi Sad', 45.2580, 19.7980),
    (8511, 'Klisa, Novi Sad', 45.2700, 19.7850),
    (8512, 'Adamovicevo Naselje, Novi Sad', 45.2350, 19.8550)
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    lat = VALUES(lat),
    lng = VALUES(lng);

INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES
    (8501, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 2 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 2 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 25 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 25 MINUTE),
     8501, 8501, 8502,
     450.00, 200.00, 50.00, 'STANDARD',
     5.00, 1500, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (8502, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 18 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 18 MINUTE),
     8501, 8501, 8502,
     380.00, 200.00, 50.00, 'STANDARD',
     3.60, 1080, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (8503, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 5 DAY),
     NULL,
     DATE_SUB(NOW(), INTERVAL 5 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 30 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 30 MINUTE),
     8501, 8501, 8502,
     520.00, 200.00, 50.00, 'STANDARD',
     6.40, 1800, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (8504, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 71 HOUR),
     NULL,
     DATE_SUB(NOW(), INTERVAL 71 HOUR),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 71 HOUR), INTERVAL 22 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 71 HOUR), INTERVAL 22 MINUTE),
     8501, 8501, 8502,
     410.00, 200.00, 50.00, 'STANDARD',
     4.20, 1320, NULL,
     FALSE, FALSE, NULL, NULL, NULL, NULL),
    (8505, 'CANCELLED',
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     NULL,
     NULL, NULL, NULL,
     8501, 8501, 8502,
     0.00, 200.00, 50.00, 'STANDARD',
     NULL, NULL, NULL,
     FALSE, FALSE, 'Passenger was not at pickup location', 8501,
     NULL, NULL),
    (8506, 'ACTIVE',
     NOW(),
     NULL,
     NOW(),
     NULL,
     NULL,
     8501, 8501, 8502,
     480.00, 200.00, 50.00, 'STANDARD',
     5.60, 1440, DATE_ADD(NOW(), INTERVAL 24 MINUTE),
     FALSE, FALSE, NULL, NULL,
     NULL, NULL);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (8501, 8501, 8501, 1),
    (8502, 8501, 8502, 2),
    (8503, 8502, 8503, 1),
    (8504, 8502, 8504, 2),
    (8505, 8503, 8505, 1),
    (8506, 8503, 8506, 2),
    (8507, 8504, 8507, 1),
    (8508, 8504, 8508, 2),
    (8509, 8505, 8509, 1),
    (8510, 8505, 8510, 2),
    (8511, 8506, 8511, 1),
    (8512, 8506, 8512, 2);

INSERT INTO ride_passengers (ride_id, passenger_email) VALUES
    (8501, 'rating.passenger@test.local'),
    (8502, 'rating.passenger@test.local'),
    (8503, 'rating.passenger@test.local'),
    (8504, 'rating.passenger@test.local'),
    (8505, 'rating.passenger@test.local'),
    (8506, 'rating.passenger@test.local');

INSERT INTO reviews (ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
    (8501, 8502, 5, 4, 'Great driver, very professional! Vehicle was clean.', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =====================================================================
-- RIDE COMPLETION (IDs 9001-9002)
-- =====================================================================
DELETE FROM notifications WHERE ride_id = 9001 OR user_id IN (9001, 9002);
DELETE FROM reviews WHERE ride_id = 9001 OR passenger_id = 9002;
DELETE FROM ride_inconsistencies WHERE ride_id = 9001 OR passenger_id = 9002;
DELETE FROM panic_events WHERE ride_id = 9001 OR user_id IN (9001, 9002);
DELETE FROM ride_waypoints WHERE ride_id = 9001;
DELETE FROM ride_passengers WHERE ride_id = 9001;

INSERT INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, active, role, created_at, updated_at, dtype
) VALUES
    (9001, 'Test', 'Driver', 'driver9001@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Driver Address', '000-000', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (9002, 'Test', 'Passenger', 'passenger9002@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Passenger Address', '111-111', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    surname = VALUES(surname),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    dtype = VALUES(dtype);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES (9001, TRUE, TRUE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO passengers (user_id) VALUES (9002)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) VALUES (
    9001, 9001, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'TEST-9001', 4, FALSE, FALSE, NULL, NULL
)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    license_plate = VALUES(license_plate),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

INSERT INTO locations (id, address, lat, lng) VALUES
    (9001, 'Test Pickup Address', 45.2600, 19.8300),
    (9002, 'Test Destination Address', 45.2500, 19.8500)
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    lat = VALUES(lat),
    lng = VALUES(lng);

INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES (
    9001, 'ACTIVE', NOW(), NULL, NOW(), NULL, NULL,
    9001, 9001, 9002,
    500.00, 200.00, 50.00, 'STANDARD',
    5.50, 900, NULL,
    FALSE, FALSE, NULL, NULL,
    NULL, NULL
);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (9001, 9001, 9001, 1),
    (9002, 9001, 9002, 2);

INSERT INTO ride_passengers (ride_id, passenger_email) VALUES (9001, 'passenger9002@test.local');

-- =====================================================================
-- SAMPLE RIDES (IDs 9501-9508)
-- =====================================================================
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

INSERT INTO users (
    id, name, surname, email, password_hash, address, phone,
    profile_picture_url, blocked, active, role, created_at, updated_at, dtype
) VALUES
    (9501, 'Sample', 'Driver', 'sample.driver@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Sample Driver Address', '+381 64 950 0001', NULL, FALSE, TRUE, 'DRIVER', NOW(), NOW(), 'Driver'),
    (9502, 'Sample', 'Passenger1', 'sample.passenger1@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Sample Passenger Address 1', '+381 64 950 0002', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger'),
    (9503, 'Sample', 'Passenger2', 'sample.passenger2@test.local', 'b46ea4ca5b6cb70b2965d8483a1ba85b92644fe9f9e04a860c891d92589a1cf4',
     'Sample Passenger Address 2', '+381 64 950 0003', NULL, FALSE, TRUE, 'PASSENGER', NOW(), NOW(), 'Passenger')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    surname = VALUES(surname),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    address = VALUES(address),
    phone = VALUES(phone),
    profile_picture_url = VALUES(profile_picture_url),
    blocked = VALUES(blocked),
    role = VALUES(role),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    dtype = VALUES(dtype);

INSERT INTO drivers (user_id, active_driver, busy, last_state_change_at)
VALUES (9501, TRUE, TRUE, NOW())
ON DUPLICATE KEY UPDATE
    active_driver = VALUES(active_driver),
    busy = VALUES(busy),
    last_state_change_at = VALUES(last_state_change_at);

INSERT INTO passengers (user_id) VALUES (9502), (9503)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO vehicles (
    id, driver_id, vehicle_type_id, license_plate, num_seats,
    baby_friendly, pet_friendly, current_lat, current_lng
) VALUES (
    9501, 9501, (SELECT id FROM vehicle_types WHERE name = 'STANDARD'),
    'SMP-9501', 4, FALSE, FALSE, 45.26, 19.84
)
ON DUPLICATE KEY UPDATE
    driver_id = VALUES(driver_id),
    vehicle_type_id = VALUES(vehicle_type_id),
    license_plate = VALUES(license_plate),
    num_seats = VALUES(num_seats),
    baby_friendly = VALUES(baby_friendly),
    pet_friendly = VALUES(pet_friendly),
    current_lat = VALUES(current_lat),
    current_lng = VALUES(current_lng);

INSERT INTO locations (id, address, lat, lng) VALUES
    (9501, 'Pickup Ride 1', 45.2600, 19.8300),
    (9502, 'Destination Ride 1', 45.2500, 19.8500),
    (9503, 'Pickup Ride 2', 45.2550, 19.8350),
    (9504, 'Destination Ride 2', 45.2450, 19.8450),
    (9505, 'Pickup Ride 3', 45.2620, 19.8280),
    (9506, 'Destination Ride 3', 45.2520, 19.8520),
    (9507, 'Pickup Ride 4', 45.2580, 19.8380),
    (9508, 'Destination Ride 4', 45.2480, 19.8480)
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    lat = VALUES(lat),
    lng = VALUES(lng);

INSERT INTO rides (
    id, status, requested_at, scheduled_for, start_time, end_time, paid_at,
    driver_id, vehicle_id, ordering_passenger_id,
    total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name,
    total_distance_km, estimated_duration_sec, estimated_arrival_at,
    baby_transport, pet_transport, cancel_reason, canceled_by_user_id,
    stopped_at, stop_location_id
) VALUES
    (9501, 'PENDING', NOW(), NULL, NULL, NULL, NULL,
     NULL, NULL, 9502,
     350.00, 200.00, 50.00, 'STANDARD',
     3.00, 600, NULL,
     FALSE, FALSE, NULL, NULL,
     NULL, NULL),
    (9502, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 15 MINUTE), NULL, NULL, NULL, NULL,
     9501, 9501, 9502,
     420.00, 200.00, 50.00, 'STANDARD',
     4.40, 780, NULL,
     FALSE, FALSE, NULL, NULL,
     NULL, NULL),
    (9503, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 5 MINUTE), NULL, NULL,
     9501, 9501, 9502,
     480.00, 200.00, 50.00, 'STANDARD',
     5.60, 900, DATE_ADD(NOW(), INTERVAL 10 MINUTE),
     FALSE, FALSE, NULL, NULL,
     NULL, NULL),
    (9504, 'FINISHED',
     DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 22 MINUTE),
     DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 22 MINUTE),
     9501, 9501, 9502,
     520.00, 200.00, 50.00, 'STANDARD',
     6.40, 1320, NULL,
     FALSE, FALSE, NULL, NULL,
     NULL, 9508);

INSERT INTO ride_waypoints (id, ride_id, location_id, waypoint_order) VALUES
    (9501, 9501, 9501, 1),
    (9502, 9501, 9502, 2),
    (9503, 9502, 9503, 1),
    (9504, 9502, 9504, 2),
    (9505, 9503, 9505, 1),
    (9506, 9503, 9506, 2),
    (9507, 9504, 9507, 1),
    (9508, 9504, 9508, 2);

INSERT INTO ride_passengers (ride_id, passenger_email) VALUES
    (9501, 'sample.passenger1@test.local'),
    (9502, 'sample.passenger1@test.local'),
    (9503, 'sample.passenger1@test.local'),
    (9503, 'sample.passenger2@test.local'),
    (9504, 'sample.passenger1@test.local');
