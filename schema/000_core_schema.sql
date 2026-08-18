-- 小兰 AI Travel 完整基础建表脚本。
-- 适用于 MySQL 8.0+ 的全新 ai_travel 数据库；已有表不会被删除或覆盖。
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

CREATE TABLE IF NOT EXISTS attraction_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, attraction_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_user_attraction(user_id,attraction_id),
  KEY idx_user_id(user_id), KEY idx_attraction_id(attraction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hotel (
  id BIGINT NOT NULL AUTO_INCREMENT, hotel_name VARCHAR(150) NOT NULL, city VARCHAR(50), address VARCHAR(255),
  longitude DECIMAL(10,6), latitude DECIMAL(10,6), price_range VARCHAR(50), rating DECIMAL(3,1), hotel_type VARCHAR(50),
  nearby_business VARCHAR(500), nearby_food VARCHAR(500), nearby_transport VARCHAR(500), facility_tags VARCHAR(500),
  description TEXT, image_url VARCHAR(500), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id)
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
  end_day_offset TINYINT NOT NULL DEFAULT 0,
  city_code VARCHAR(20), transport_mode VARCHAR(30), distance_from_prev INT, travel_time_from_prev INT, description VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_plan_day_order(plan_id,day_number,item_order), KEY idx_item_plan(plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, plan_id BIGINT NOT NULL,
  feedback_stage VARCHAR(30) NOT NULL, feedback_type VARCHAR(50), content TEXT NOT NULL,
  feedback_action VARCHAR(50), ai_summary TEXT, feedback_importance INT DEFAULT 1,
  source VARCHAR(30), is_memory_candidate TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_user_id(user_id), KEY idx_plan_id(plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_memory (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, memory_type VARCHAR(30) NOT NULL, memory_content VARCHAR(500) NOT NULL,
  source_feedback_id BIGINT, user_confirmed TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_user_id(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_hotel_recommend (
  id BIGINT NOT NULL AUTO_INCREMENT, plan_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL,
  recommend_category VARCHAR(50), sort_order INT, recommend_score DECIMAL(5,2),
  distance_score DECIMAL(5,2), price_score DECIMAL(5,2), comfort_score DECIMAL(5,2), food_score DECIMAL(5,2),
  recommend_reason TEXT, is_selected TINYINT(1) DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_plan_id(plan_id), KEY idx_hotel_id(hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
