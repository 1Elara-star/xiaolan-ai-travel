-- Fresh-install schema for the non-AI product foundation.
-- Review and execute against MySQL 8.0+. Existing tables are preserved.
CREATE TABLE IF NOT EXISTS user (
  id BIGINT NOT NULL AUTO_INCREMENT, username VARCHAR(50) NOT NULL, password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50), avatar VARCHAR(255), phone VARCHAR(20), email VARCHAR(100), role VARCHAR(20) NOT NULL DEFAULT 'USER',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_user_username(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_profile (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, mbti VARCHAR(4), travel_pace VARCHAR(30), budget_preference VARCHAR(30),
  transport_preference VARCHAR(30), interest_tags VARCHAR(500), dislike_tags VARCHAR(500), special_notes VARCHAR(500),
  companion_preference VARCHAR(100), food_preference VARCHAR(500), meal_style_preference VARCHAR(500), restaurant_preference VARCHAR(500), accommodation_preference VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_profile_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attraction (
  id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(150) NOT NULL, city VARCHAR(100) NOT NULL, address VARCHAR(255), longitude DECIMAL(10,7), latitude DECIMAL(10,7),
  type VARCHAR(50), description TEXT, feature_description TEXT, story_background TEXT, suitable_tags VARCHAR(500), avoid_tags VARCHAR(500), suggest_duration INT,
  open_time VARCHAR(100), ticket_info VARCHAR(255), image_url VARCHAR(500), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_attraction_city(city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_plan (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, title VARCHAR(100) NOT NULL, departure_city VARCHAR(100) NOT NULL, destination VARCHAR(100) NOT NULL,
  start_date DATE NOT NULL, end_date DATE NOT NULL, travel_days INT NOT NULL, people_count INT NOT NULL, companion_type VARCHAR(30), budget DECIMAL(12,2), trip_type VARCHAR(50),
  trip_preferences VARCHAR(500), special_requirements VARCHAR(1000), plan_content LONGTEXT, trip_status VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_plan_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_plan_item (
  id BIGINT NOT NULL AUTO_INCREMENT, plan_id BIGINT NOT NULL, day_number INT NOT NULL, item_order INT NOT NULL, item_type VARCHAR(30) NOT NULL,
  attraction_id BIGINT, place_name VARCHAR(150) NOT NULL, address VARCHAR(255), longitude DECIMAL(10,7), latitude DECIMAL(10,7), start_time TIME, end_time TIME,
  transport_mode VARCHAR(30), distance_from_prev INT, travel_time_from_prev INT, description VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_plan_day_order(plan_id,day_number,item_order), KEY idx_item_plan(plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_memory (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, memory_type VARCHAR(30) NOT NULL, content VARCHAR(500) NOT NULL,
  source VARCHAR(100), confirmed TINYINT(1) NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_memory_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
