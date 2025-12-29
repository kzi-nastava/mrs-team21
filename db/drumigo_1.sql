-- Drumigo Seed Data
-- Insert test data for demo purposes

SET FOREIGN_KEY_CHECKS = 0;

-- Insert vehicle types
INSERT INTO vehicle_types (id, name, start_price, price_per_km) VALUES
(1, 'STANDARD', 120.00, 120.00),
(2, 'LUXURY', 200.00, 150.00),
(3, 'VAN', 180.00, 130.00);

-- Insert users (base table)
INSERT INTO users (id, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, active, dtype, created_at, updated_at) VALUES
-- Admin
(1, 'Admin', 'User', 'admin@drumigo.com', NULL, 'Admin Street 1', '+381601234567', NULL, FALSE, 'ADMIN', TRUE, 'Admin', NOW(), NOW()),

-- Drivers
(2, 'Marko', 'Jovanovic', 'marko.driver@drumigo.com', NULL, 'Driver Street 5', '+381612345678', NULL, FALSE, 'DRIVER', TRUE, 'Driver', NOW(), NOW()),
(3, 'Petar', 'Petrovic', 'petar.driver@drumigo.com', NULL, 'Driver Avenue 10', '+381623456789', NULL, FALSE, 'DRIVER', TRUE, 'Driver', NOW(), NOW()),
(4, 'Nikola', 'Nikolic', 'nikola.driver@drumigo.com', NULL, 'Road Street 15', '+381634567890', NULL, FALSE, 'DRIVER', FALSE, 'Driver', NOW(), NOW()),

-- Passengers
(5, 'Ana', 'Anic', 'ana.passenger@drumigo.com', NULL, 'Passenger Street 20', '+381645678901', NULL, FALSE, 'PASSENGER', TRUE, 'Passenger', NOW(), NOW()),
(6, 'Jovana', 'Jovanic', 'jovana.passenger@drumigo.com', NULL, 'Passenger Avenue 25', '+381656789012', NULL, FALSE, 'PASSENGER', TRUE, 'Passenger', NOW(), NOW()),
(7, 'Stefan', 'Stefanovic', 'stefan.passenger@drumigo.com', NULL, 'Passenger Road 30', '+381667890123', NULL, FALSE, 'PASSENGER', TRUE, 'Passenger', NOW(), NOW()),
(8, 'Milica', 'Milicic', 'milica.passenger@drumigo.com', NULL, 'Passenger Lane 35', '+381678901234', NULL, FALSE, 'PASSENGER', TRUE, 'Passenger', NOW(), NOW());

-- Insert admins
INSERT INTO admins (user_id) VALUES (1);

-- Insert drivers
INSERT INTO drivers (user_id, license_number, active_driver, last_state_change_at) VALUES
(2, 'DL123456', TRUE, NOW()),
(3, 'DL234567', TRUE, NOW()),
(4, 'DL345678', FALSE, NOW());

-- Insert passengers
INSERT INTO passengers (user_id) VALUES
(5), (6), (7), (8);

-- Insert vehicles
INSERT INTO vehicles (id, driver_id, vehicle_type_id, model, license_plate, num_seats, baby_friendly, pet_friendly, current_lat, current_lng, available) VALUES
(1, 2, 1, 'Toyota Corolla', 'BG-123-AB', 4, TRUE, FALSE, 44.7866, 20.4489, TRUE),
(2, 3, 2, 'Mercedes E-Class', 'BG-456-CD', 4, TRUE, TRUE, 44.7876, 20.4499, TRUE),
(3, 4, 3, 'Ford Transit', 'BG-789-EF', 8, FALSE, TRUE, 44.7886, 20.4509, TRUE);

-- Insert locations
INSERT INTO locations (id, address, lat, lng) VALUES
(1, 'Knez Mihailova 1, Belgrade', 44.8179, 20.4573),
(2, 'Terazije 1, Belgrade', 44.8120, 20.4590),
(3, 'Slavija Square, Belgrade', 44.8043, 20.4643),
(4, 'Zemun Market, Belgrade', 44.8456, 20.4056),
(5, 'Ada Ciganlija, Belgrade', 44.7866, 20.4489),
(6, 'Nikola Tesla Airport, Belgrade', 44.8187, 20.2869),
(7, 'New Belgrade, Block 1', 44.8127, 20.4218),
(8, 'Vracar, Belgrade', 44.8014, 20.4782);

-- Insert rides
INSERT INTO rides (id, status, requested_at, scheduled_for, start_time, end_time, driver_id, vehicle_id, total_cost, pricing_start_price, pricing_price_per_km, pricing_vehicle_type_name, total_distance_km, estimated_duration_sec, estimated_arrival_at, baby_transport, pet_transport, cancel_reason, canceled_by_user_id, stopped_at, stop_location_id) VALUES
-- Active ride
(1, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL, 2, 1, NULL, 120.00, 120.00, 'STANDARD', NULL, 1800, DATE_ADD(NOW(), INTERVAL 10 MINUTE), FALSE, FALSE, NULL, NULL, NULL, NULL),

-- Finished ride
(2, 'FINISHED', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 2, 1, 360.00, 120.00, 120.00, 'STANDARD', 2.0, 1800, NULL, FALSE, FALSE, NULL, NULL, NULL, NULL),

-- Finished ride with driver 3
(3, 'FINISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR), 3, 2, 500.00, 200.00, 150.00, 'LUXURY', 2.0, 2100, NULL, TRUE, TRUE, NULL, NULL, NULL, NULL),

-- Pending ride
(4, 'PENDING', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, FALSE, FALSE, NULL, NULL, NULL, NULL),

-- Accepted ride
(5, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 10 MINUTE), NULL, NULL, NULL, 3, 2, NULL, 200.00, 150.00, 'LUXURY', NULL, 2400, DATE_ADD(NOW(), INTERVAL 30 MINUTE), TRUE, FALSE, NULL, NULL, NULL, NULL);

-- Insert ride waypoints
INSERT INTO ride_waypoints (ride_id, location_id, waypoint_order) VALUES
(1, 1, 0), -- Start
(1, 3, 1), -- Destination
(2, 2, 0), -- Start
(2, 4, 1), -- Destination
(3, 1, 0), -- Start
(3, 6, 1), -- Destination
(4, 5, 0), -- Start
(4, 7, 1), -- Destination
(5, 3, 0), -- Start
(5, 8, 1); -- Destination

-- Insert ride passengers (linked passengers)
INSERT INTO ride_passengers (ride_id, passenger_id) VALUES
(1, 6), -- Ride 1 has linked passenger Jovana
(2, 7), -- Ride 2 has linked passenger Stefan
(3, 6), -- Ride 3 has linked passenger Jovana
(3, 7), -- Ride 3 has linked passenger Stefan
(4, 8); -- Ride 4 has linked passenger Milica

-- Insert notifications
INSERT INTO notifications (id, user_id, ride_id, type, message, created_at, read_at) VALUES
(1, 5, 1, 'RIDE_STARTED', 'Your ride has started', DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL),
(2, 6, 1, 'LINKED_TO_RIDE', 'You have been added to a ride', DATE_SUB(NOW(), INTERVAL 25 MINUTE), NULL),
(3, 5, 2, 'RIDE_FINISHED', 'Your ride has finished', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(4, 7, 2, 'LINKED_TO_RIDE', 'You have been added to a ride', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL),
(5, 5, 5, 'RIDE_ACCEPTED', 'Your ride request has been accepted', DATE_SUB(NOW(), INTERVAL 5 MINUTE), NULL);

-- Insert ride inconsistencies
INSERT INTO ride_inconsistencies (id, ride_id, passenger_id, note, created_at) VALUES
(1, 2, 5, 'Driver took a longer route than necessary', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(2, 2, 7, 'Vehicle was dirty inside', DATE_SUB(NOW(), INTERVAL 45 MINUTE));

-- Insert reviews
INSERT INTO reviews (id, ride_id, passenger_id, rating_driver, rating_vehicle, comment, created_at) VALUES
(1, 2, 5, 4, 5, 'Good driver, clean vehicle', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(2, 3, 5, 5, 5, 'Excellent service, very comfortable', DATE_SUB(NOW(), INTERVAL 20 HOUR));

-- Insert panic events
INSERT INTO panic_events (id, ride_id, user_id, reason, created_at) VALUES
(1, 2, 7, 'Driver was driving recklessly', DATE_SUB(NOW(), INTERVAL 55 MINUTE));

-- Insert messages (support chat)
INSERT INTO messages (id, sender_id, receiver_id, content, type, created_at) VALUES
(1, 5, 1, 'Hello, I need help with my ride', 'SUPPORT', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 5, 'How can I assist you?', 'SUPPORT', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(3, 5, 1, 'The driver was late', 'SUPPORT', DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(4, 6, 1, 'I want to report an issue', 'SUPPORT', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(5, 1, 6, 'Please describe the issue', 'SUPPORT', DATE_SUB(NOW(), INTERVAL 11 HOUR));

-- Insert favorite routes
INSERT INTO favorite_routes (id, passenger_id, vehicle_type_id, baby_transport, pet_transport, created_at) VALUES
(1, 5, 1, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 6, 2, TRUE, FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Insert favorite route waypoints
INSERT INTO favorite_route_waypoints (favorite_route_id, location_id, waypoint_order) VALUES
(1, 1, 0), -- Start
(1, 3, 1), -- Destination
(2, 5, 0), -- Start
(2, 6, 1); -- Destination

-- Insert user notes
INSERT INTO user_notes (id, user_id, admin_id, note, created_at) VALUES
(1, 2, 1, 'Driver has good ratings', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 7, 1, 'Reported multiple issues', DATE_SUB(NOW(), INTERVAL 5 DAY));

-- Insert driver documents
INSERT INTO driver_documents (id, driver_id, document_name, document_url, uploaded_at) VALUES
(1, 2, 'Driver License', 'https://example.com/docs/dl123456.pdf', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2, 2, 'Vehicle Registration', 'https://example.com/docs/vr123456.pdf', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(3, 3, 'Driver License', 'https://example.com/docs/dl234567.pdf', DATE_SUB(NOW(), INTERVAL 25 DAY));

SET FOREIGN_KEY_CHECKS = 1;

