-- 支持夜班交通、跨午夜活动等跨天行程节点。
-- 0 表示当天结束，1 表示次日结束；旧数据默认按当天结束处理。
ALTER TABLE travel_plan_item
  ADD COLUMN end_day_offset TINYINT NOT NULL DEFAULT 0 AFTER end_time;
