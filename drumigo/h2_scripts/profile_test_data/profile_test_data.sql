-- H2 Test Data for Profile Page Testing
-- Run this script in H2 Console to create test users with different roles

-- Clear existing test data (in reverse dependency order)
DELETE FROM vehicles WHERE id IN (1, 2, 3);
DELETE FROM drivers WHERE user_id IN (2, 4, 5);
DELETE FROM passengers WHERE user_id IN (1, 6);
DELETE FROM admins WHERE user_id IN (3);
DELETE FROM users WHERE id IN (1, 2, 3, 4, 5, 6);
DELETE FROM vehicle_types WHERE id IN (1, 2, 3);

-- Insert Vehicle Types first (required for vehicles)
INSERT INTO vehicle_types (id, name, price_per_km, start_price) VALUES
(1, 'STANDARD', 1.50, 2.00),
(2, 'LUXURY', 2.50, 5.00),
(3, 'VAN', 2.00, 3.50);

-- Insert Passenger User (Password: Password123)
INSERT INTO users (id, dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, created_at, updated_at) VALUES
(1, 'Passenger', 'Ana', 'Petrović', 'ana.petrovic@example.com', '008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601', 'Bulevar Oslobođenja 10, Novi Sad', '+381641234567', null, false, 'PASSENGER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO passengers (user_id) VALUES (1);

-- Insert Driver User (with vehicle) (Password: Password123)
INSERT INTO users (id, dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, created_at, updated_at) VALUES
(2, 'Driver', 'Marko', 'Jovanović', 'marko.jovanovic@example.com', '008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601', 'Narodnih Heroja 25, Beograd', '+381652345678', null, false, 'DRIVER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO drivers (user_id, active_driver, last_state_change_at) VALUES
(2, true, CURRENT_TIMESTAMP);

-- Insert Vehicle for Driver
INSERT INTO vehicles (id, driver_id, vehicle_type_id, num_seats, baby_friendly, pet_friendly, current_lat, current_lng) VALUES
(1, 2, 1, 4, true, false, 45.2671, 19.8335);

-- Insert Admin User (Password: Password123)
INSERT INTO users (id, dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, created_at, updated_at) VALUES
(3, 'Admin', 'Stefan', 'Nikolić', 'stefan.nikolic@drumigo.com', '008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601', 'Trg Republike 1, Beograd', '+381663456789', null, false, 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO admins (user_id) VALUES (3);

-- Insert another Driver with different vehicle type (Luxury) (Password: Password123)
INSERT INTO users (id, dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, created_at, updated_at) VALUES
(4, 'Driver', 'Jelena', 'Milošević', 'jelena.milosevic@example.com', '008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601', 'Knez Mihailova 30, Beograd', '+381641112222', null, false, 'DRIVER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO drivers (user_id, active_driver, last_state_change_at) VALUES
(4, false, CURRENT_TIMESTAMP);

-- Insert Luxury Vehicle for Driver Jelena
INSERT INTO vehicles (id, driver_id, vehicle_type_id, num_seats, baby_friendly, pet_friendly, current_lat, current_lng) VALUES
(2, 4, 2, 4, false, true, 44.7866, 20.4489);

-- Insert Driver with VAN (baby and pet friendly) (Password: Password123)
INSERT INTO users (id, dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, created_at, updated_at) VALUES
(5, 'Driver', 'Nikola', 'Đorđević', 'nikola.djordjevic@example.com', '008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601', 'Cara Dušana 15, Niš', '+381652223333', null, false, 'DRIVER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO drivers (user_id, active_driver, last_state_change_at) VALUES
(5, true, CURRENT_TIMESTAMP);

-- Insert VAN Vehicle
INSERT INTO vehicles (id, driver_id, vehicle_type_id, num_seats, baby_friendly, pet_friendly, current_lat, current_lng) VALUES
(3, 5, 3, 7, true, true, 43.3209, 21.8954);

-- Insert another Passenger (Password: Password123)
INSERT INTO users (id, dtype, name, surname, email, password_hash, address, phone, profile_picture_url, blocked, role, created_at, updated_at) VALUES
(6, 'Passenger', 'Milica', 'Stojanović', 'milica.stojanovic@example.com', '008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601', 'Kraljevića Marka 5, Kragujevac', '+381663334444', null, false, 'PASSENGER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO passengers (user_id) VALUES (6);

COMMIT;
