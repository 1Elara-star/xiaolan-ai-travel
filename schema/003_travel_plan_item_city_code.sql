-- 高德公交路径规划要求传入起点和终点的城市编码。
-- city_code 在 POI 匹配时由后端从高德结果中保存，不由前端填写。
ALTER TABLE travel_plan_item
  ADD COLUMN city_code VARCHAR(20) NULL AFTER latitude;
