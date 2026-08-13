-- Review and execute manually against the ai_travel database.
-- The application intentionally does not auto-run schema migrations.
CREATE TABLE IF NOT EXISTS attraction_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    attraction_id BIGINT NOT NULL COMMENT '景点ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_attraction (user_id, attraction_id),
    KEY idx_user_id (user_id),
    KEY idx_attraction_id (attraction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户景点收藏表';
