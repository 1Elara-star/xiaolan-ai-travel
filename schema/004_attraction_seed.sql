-- 小兰 AI Travel 城市探索基础景点数据。
-- 仅在同城市、同名称景点不存在时插入，可安全重复执行。

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '鼓浪屿', '厦门', '海岛漫步',
  '适合沿着小路慢慢感受老建筑、海岸线和安静院落。',
  '老别墅、海岸线与音乐文化共同组成海岛体验。',
  '岛上的近代建筑、领事馆旧址与音乐文化，共同留下了厦门面向海洋的历史。',
  '世界遗产,建筑,海岛', '节假日客流较大', 360,
  '开放与船班时间请以官方最新公告为准', '登岛船票请提前确认',
  '/images/cities/xiamen/gulangyu.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '厦门' AND name = '鼓浪屿');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '沙坡尾', '厦门', '文艺街区',
  '老渔船、临水街区与年轻小店共存，适合傍晚散步。',
  '老渔港记忆与当代艺术空间交织。',
  '这里曾是厦门港的重要避风坞，如今保留渔港记忆，也长出了新的艺术空间。',
  '渔港,文艺,日落', '周末客流较多', 150,
  '街区开放时间以各场所为准', '街区游览通常无需门票',
  '/images/cities/xiamen/shapowei.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '厦门' AND name = '沙坡尾');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '南普陀寺', '厦门', '历史人文',
  '寺院、山林与校园相邻，适合安静参观。',
  '山海之间的古寺与闽南宗教建筑。',
  '寺院始建于唐末，清代重建后因供奉观音，与浙江普陀山相呼应而得名。',
  '古寺,人文,素斋', '需尊重宗教场所秩序', 120,
  '开放时间请以官方最新公告为准', '参观信息请以现场公告为准',
  '/images/cities/xiamen/nanputuo.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '厦门' AND name = '南普陀寺');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '集美学村', '厦门', '经典地标',
  '学村建筑与海岸相连，适合了解嘉庚文化。',
  '闽南屋顶与西式结构融合的嘉庚建筑群。',
  '陈嘉庚先生兴学留下的建筑群，将闽南屋顶与西式结构融合在一起。',
  '建筑,校园,海岸', '区域较大需要步行', 210,
  '公共区域开放情况请以现场为准', '公共区域通常无需门票',
  '/images/cities/xiamen/jimei.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '厦门' AND name = '集美学村');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '宽窄巷子', '成都', '市井生活',
  '建筑、茶馆和小吃集中，适合建立对成都老城的第一印象。',
  '川西院落与成都街巷生活。',
  '三条平行街巷由清代驻防城演变而来，院落格局记录了成都老城的生活方式。',
  '街巷,院落,小吃', '主街商业化程度较高', 150,
  '街区开放时间以各场所为准', '街区游览通常无需门票',
  '/images/cities/chengdu/kuanzhai.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '成都' AND name = '宽窄巷子');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '武侯祠', '成都', '历史人文',
  '历史人物、碑刻和园林结合，是理解三国文化的重要入口。',
  '三国文化与红墙竹影相结合。',
  '这里是纪念刘备、诸葛亮等蜀汉人物的祠庙，也是理解三国文化的重要入口。',
  '三国,历史,园林', '节假日客流较大', 150,
  '开放时间请以官方最新公告为准', '票务信息请以官方最新公告为准',
  '/images/cities/chengdu/wuhou.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '成都' AND name = '武侯祠');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '杜甫草堂', '成都', '历史人文',
  '竹林、溪水和文学故事结合，适合慢慢参观。',
  '文学纪念地与安静园林。',
  '杜甫在成都居住近四年，并在这里写下两百余首诗，园林保存着后人对诗意生活的想象。',
  '文学,园林,安静', '园区内容较多需预留时间', 150,
  '开放时间请以官方最新公告为准', '票务信息请以官方最新公告为准',
  '/images/cities/chengdu/dufu.webp'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '成都' AND name = '杜甫草堂');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '鹤鸣茶社', '成都', '轻松慢游',
  '适合喝茶、聊天并观察成都的日常生活节奏。',
  '人民公园里的老茶社与盖碗茶文化。',
  '人民公园里的老茶社延续着盖碗茶、聊天与晒太阳的城市日常。',
  '茶馆,市井,慢生活', '热门时段座位紧张', 120,
  '营业时间请以现场最新信息为准', '消费信息请以现场为准',
  '/images/cities/chengdu/teahouse.webp'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '成都' AND name = '鹤鸣茶社');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '拙政园', '苏州', '古典园林',
  '以水面为中心，通过亭台、廊桥和借景呈现江南园林空间。',
  '江南古典园林代表。',
  '明代园林以水面为中心，用亭台、廊桥和借景营造移步换景的体验。',
  '世界遗产,园林,建筑', '热门时段客流较大', 150,
  '开放时间请以官方最新公告为准', '通常需要预约购票',
  '/images/cities/suzhou/garden.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '苏州' AND name = '拙政园');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '平江路', '苏州', '水巷古街',
  '街河并行，小桥、流水和居民生活共同形成江南印象。',
  '沿河生活的老街与水巷。',
  '街河并行的格局延续数百年，评弹、手艺与居民生活仍在这里发生。',
  '水巷,古街,评弹', '主街热门时段拥挤', 180,
  '街区全天开放，各场所时间不同', '街区游览通常无需门票',
  '/images/cities/suzhou/pingjiang.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '苏州' AND name = '平江路');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '虎丘', '苏州', '历史故事',
  '自然坡地、古塔和吴地故事结合，适合历史与建筑爱好者。',
  '古塔、坡地与吴地传说。',
  '虎丘承载吴地传说，云岩寺塔历经千年并逐渐倾斜，成为城市历史坐标。',
  '古塔,传说,登高', '存在台阶和坡路', 150,
  '开放时间请以官方最新公告为准', '票务信息请以官方最新公告为准',
  '/images/cities/suzhou/tiger-hill.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '苏州' AND name = '虎丘');

INSERT INTO attraction
  (name, city, type, description, feature_description, story_background, suitable_tags,
   avoid_tags, suggest_duration, open_time, ticket_info, image_url)
SELECT
  '苏州博物馆', '苏州', '建筑美学',
  '现代几何建筑与传统苏州园林意象相结合。',
  '贝聿铭设计的现代江南建筑。',
  '贝聿铭用几何屋顶、白墙和水院重新解释传统苏州建筑。',
  '贝聿铭,博物馆,建筑', '通常需要提前预约', 150,
  '开放时间和闭馆安排请以官方最新公告为准', '预约信息请以官方最新公告为准',
  '/images/cities/suzhou/museum.jpg'
WHERE NOT EXISTS (SELECT 1 FROM attraction WHERE city = '苏州' AND name = '苏州博物馆');
