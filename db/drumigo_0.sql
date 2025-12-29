-- Drumigo Database Schema
-- Charset: utf8mb4, Collation: utf8mb4_general_ci

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Base users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    phone VARCHAR(50) NULL,
    profile_picture_url VARCHAR(500) NULL,
    blocked BOOLEAN DEFAULT FALSE NOT NULL,
    role ENUM('PASSENGER', 'DRIVER', 'ADMIN') NOT NULL,
    active BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    dtype VARCHAR(31) NULL,
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_blocked (blocked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Passengers table
CREATE TABLE IF NOT EXISTS passengers (
    user_id BIGINT PRIMARY KEY,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Drivers table
CREATE TABLE IF NOT EXISTS drivers (
    user_id BIGINT PRIMARY KEY,
    license_number VARCHAR(50) NULL,
    active_driver BOOLEAN DEFAULT FALSE NOT NULL,
    last_state_change_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_active_driver (active_driver)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Admins table
CREATE TABLE IF NOT EXISTS admins (
    user_id BIGINT PRIMARY KEY,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- User tokens table
CREATE TABLE IF NOT EXISTS user_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    type ENUM('PASSENGER_ACTIVATION', 'DRIVER_SET_PASSWORD', 'PASSWORD_RESET') NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token_hash (token_hash),
    INDEX idx_user_id_type (user_id, type),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Vehicle types table
CREATE TABLE IF NOT EXISTS vehicle_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name ENUM('STANDARD', 'LUXURY', 'VAN') NOT NULL UNIQUE,
    start_price DECIMAL(10, 2) NOT NULL,
    price_per_km DECIMAL(10, 2) NOT NULL,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL UNIQUE,
    vehicle_type_id BIGINT NOT NULL,
    model VARCHAR(255) NOT NULL,
    license_plate VARCHAR(50) NOT NULL UNIQUE,
    num_seats INT NOT NULL,
    baby_friendly BOOLEAN DEFAULT FALSE NOT NULL,
    pet_friendly BOOLEAN DEFAULT FALSE NOT NULL,
    current_lat DECIMAL(10, 8) NULL,
    current_lng DECIMAL(11, 8) NULL,
    available BOOLEAN DEFAULT TRUE NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES drivers(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id),
    INDEX idx_driver_id (driver_id),
    INDEX idx_available (available),
    INDEX idx_location (current_lat, current_lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Locations table
CREATE TABLE IF NOT EXISTS locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    address VARCHAR(500) NOT NULL,
    lat DECIMAL(10, 8) NOT NULL,
    lng DECIMAL(11, 8) NOT NULL,
    INDEX idx_coordinates (lat, lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Rides table
CREATE TABLE IF NOT EXISTS rides (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'ACTIVE', 'FINISHED', 'CANCELLED') NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    scheduled_for TIMESTAMP NULL,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    driver_id BIGINT NULL,
    vehicle_id BIGINT NULL,
    ordering_passenger_id BIGINT NULL,
    total_cost DECIMAL(10, 2) NULL,
    pricing_start_price DECIMAL(10, 2) NULL,
    pricing_price_per_km DECIMAL(10, 2) NULL,
    pricing_vehicle_type_name VARCHAR(50) NULL,
    total_distance_km DECIMAL(10, 2) NULL,
    estimated_duration_sec INT NULL,
    estimated_arrival_at TIMESTAMP NULL,
    baby_transport BOOLEAN DEFAULT FALSE NOT NULL,
    pet_transport BOOLEAN DEFAULT FALSE NOT NULL,
    cancel_reason VARCHAR(500) NULL,
    canceled_by_user_id BIGINT NULL,
    stopped_at TIMESTAMP NULL,
    stop_location_id BIGINT NULL,
    FOREIGN KEY (driver_id) REFERENCES drivers(user_id) ON DELETE SET NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,
    FOREIGN KEY (ordering_passenger_id) REFERENCES passengers(user_id) ON DELETE SET NULL,
    FOREIGN KEY (canceled_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (stop_location_id) REFERENCES locations(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_driver_id (driver_id),
    INDEX idx_scheduled_for (scheduled_for),
    INDEX idx_requested_at (requested_at),
    INDEX idx_vehicle_id (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Ride waypoints table
CREATE TABLE IF NOT EXISTS ride_waypoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    waypoint_order INT NOT NULL,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
    INDEX idx_ride_id_order (ride_id, waypoint_order),
    UNIQUE KEY unique_ride_order (ride_id, waypoint_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Ride passengers table (join table)
CREATE TABLE IF NOT EXISTS ride_passengers (
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    PRIMARY KEY (ride_id, passenger_id),
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    FOREIGN KEY (passenger_id) REFERENCES passengers(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Ride inconsistencies table
CREATE TABLE IF NOT EXISTS ride_inconsistencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    FOREIGN KEY (passenger_id) REFERENCES passengers(user_id) ON DELETE CASCADE,
    INDEX idx_ride_id (ride_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Panic events table
CREATE TABLE IF NOT EXISTS panic_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_ride_id (ride_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Reviews table
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    rating_driver INT NOT NULL CHECK (rating_driver >= 1 AND rating_driver <= 5),
    rating_vehicle INT NOT NULL CHECK (rating_vehicle >= 1 AND rating_vehicle <= 5),
    comment TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    FOREIGN KEY (passenger_id) REFERENCES passengers(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_ride_passenger (ride_id, passenger_id),
    INDEX idx_ride_id (ride_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Messages table (support chat)
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    type ENUM('SUPPORT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sender_receiver_created (sender_id, receiver_id, created_at),
    INDEX idx_type_receiver_created (type, receiver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ride_id BIGINT NULL,
    type ENUM('RIDE_ACCEPTED', 'RIDE_REJECTED', 'RIDE_STARTED', 'RIDE_FINISHED', 'RIDE_CANCELLED', 'LINKED_TO_RIDE', 'PANIC_ALERT', 'SUPPORT_MESSAGE', 'SCHEDULED_RIDE_REMINDER') NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE SET NULL,
    INDEX idx_user_id_read_at (user_id, read_at),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Favorite routes table
CREATE TABLE IF NOT EXISTS favorite_routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    baby_transport BOOLEAN DEFAULT FALSE NOT NULL,
    pet_transport BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (passenger_id) REFERENCES passengers(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id) ON DELETE CASCADE,
    INDEX idx_passenger_id (passenger_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Favorite route waypoints table
CREATE TABLE IF NOT EXISTS favorite_route_waypoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    favorite_route_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    waypoint_order INT NOT NULL,
    FOREIGN KEY (favorite_route_id) REFERENCES favorite_routes(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
    INDEX idx_favorite_route_order (favorite_route_id, waypoint_order),
    UNIQUE KEY unique_favorite_route_order (favorite_route_id, waypoint_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- User notes table
CREATE TABLE IF NOT EXISTS user_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES admins(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Driver documents table
CREATE TABLE IF NOT EXISTS driver_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_url VARCHAR(500) NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES drivers(user_id) ON DELETE CASCADE,
    INDEX idx_driver_id (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Driver profile change requests table
CREATE TABLE IF NOT EXISTS driver_profile_change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    requested_changes_json TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP NULL,
    reviewed_by_admin_id BIGINT NULL,
    FOREIGN KEY (driver_id) REFERENCES drivers(user_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by_admin_id) REFERENCES admins(user_id) ON DELETE SET NULL,
    INDEX idx_driver_id (driver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;

