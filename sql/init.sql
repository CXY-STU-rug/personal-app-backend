-- ============================================================================
-- 本文件是表结构的【唯一真源】，personaluseapp/sql/init.sql 是它的副本
-- （Docker 起 MySQL 容器时会自动执行那份，见 docker-compose.yml 的
--   ./sql:/docker-entrypoint-initdb.d 挂载）。改了这里记得同步过去。
--
-- 已和本地 personal_app 库逐项校验过：85 个字段、31 个索引完全一致。
-- ⚠ 以后每次 ALTER TABLE 改了表结构，都要回来改这个文件，否则会"漂移"——
--    新环境用旧脚本建库，应用能正常启动（MyBatis 不校验表结构），
--    但一调接口就报 Unknown column，很难查。
--    校验方法：把库名换成临时库跑一遍，再 diff 两个库的 information_schema。
-- ============================================================================

-- 建库：utf8mb4支持中文和emoji，utf8只支持3字节，存不了emoji
CREATE DATABASE IF NOT EXISTS `personal_app` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `personal_app`;

-- 用户表
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '用户ID，主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号，唯一',
  `password` VARCHAR(100) NOT NULL COMMENT '密码，bcrypt加密后存储，绝不能存明文',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称，展示用',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱，用于找回密码，可空',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像图片URL',
  `created_at` DATETIME NOT NULL COMMENT '注册时间',
  `updated_at` DATETIME NOT NULL COMMENT '信息更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),  -- 唯一索引，防止账号重复注册
  -- email 也建唯一索引：找回密码要靠邮箱定位用户，一个邮箱只能绑一个账号。
  -- 注意 MySQL 的唯一索引允许多个 NULL，所以"没填邮箱的用户"不会互相冲突；
  -- 但空字符串 '' 是具体值，只能有一行——OAuth 注册的用户宁可存 NULL 也别存 ''
  UNIQUE KEY `uk_email` (`email`)
) COMMENT='用户表';

-- 待办任务表
CREATE TABLE `todos` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '任务ID，主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID，做数据隔离用，每条SQL都必须带这个过滤条件',
  `title` VARCHAR(100) NOT NULL COMMENT '任务标题',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间，允许为空（不设截止时间）',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1=高 2=中 3=低',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '完成状态：0=未完成 1=已完成',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)   -- 几乎每条查询都按user_id过滤，不建索引会全表扫描
) COMMENT='待办任务表';

-- 记账分类表（V1先用系统预置的固定分类，不支持用户自定义）
CREATE TABLE `finance_categories` (
  `id` INT UNSIGNED AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(20) NOT NULL COMMENT '分类名称，如餐饮/交通/工资',
  `type` TINYINT NOT NULL COMMENT '分类类型：1=支出 2=收入，收支分类要分开，不能混用',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '分类图标名，前端展示用',
  PRIMARY KEY (`id`)
) COMMENT='记账分类表，系统预置数据，不属于某个用户';

-- 记账记录表
CREATE TABLE `finance_records` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
  `category_id` INT UNSIGNED NOT NULL COMMENT '分类ID，关联finance_categories表',
  `type` TINYINT NOT NULL COMMENT '收支类型：1=支出 2=收入，冗余存一份，避免统计时每次联表查分类',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额，用DECIMAL不用float/double，避免小数精度丢失',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `record_time` DATETIME NOT NULL COMMENT '记账发生时间（不是创建时间，用户可能补记几天前的账）',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `record_time`)   -- 月度统计要按user_id+时间范围查，联合索引
) COMMENT='记账记录表';

-- 笔记表
CREATE TABLE `notes` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '笔记ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '正文，存Markdown源码，前端负责渲染',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) COMMENT='笔记表';

-- 笔记标签表
CREATE TABLE `note_tags` (
  `id` INT UNSIGNED AUTO_INCREMENT COMMENT '标签ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '标签属于哪个用户，不同用户的标签互相独立',
  `name` VARCHAR(20) NOT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`)
) COMMENT='笔记标签表';

-- 笔记与标签的关联表（多对多：一篇笔记可以打多个标签，一个标签可以用在多篇笔记上）
CREATE TABLE `note_tag_relations` (
  `note_id` BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
  `tag_id` INT UNSIGNED NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`note_id`, `tag_id`)   -- 联合主键，天然防止同一篇笔记重复打同一个标签
) COMMENT='笔记与标签的多对多关联表';

-- 日程表（V2 日历模块新增）
-- 为什么不复用 todos：待办是"某个时刻截止的任务"（时间点），日程是"占用一段时间的安排"（时间段），
-- 冲突检测只对时间段有意义，语义不同就分表
CREATE TABLE `schedules` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '日程ID，主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID，数据隔离',
  `title` VARCHAR(100) NOT NULL COMMENT '日程标题',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间；应用层校验：必须晚于开始时间，且和开始时间是同一天（V2不做跨天日程，简化月视图和冲突逻辑）',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_start` (`user_id`, `start_time`)   -- 月视图和冲突检测都按 user_id+时间范围查
) COMMENT='日程表（V2日历模块）';

-- AI助手对话记录表（V3 新增）
-- 为什么天气不建表、AI要建表：天气是"别人家的、会过期的"数据，放 Redis 带 TTL 即可；
-- 对话记录是"用户自己产生的、要长期回看的"数据，才配进 MySQL
CREATE TABLE `ai_messages` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '消息ID，主键；自增id天然就是时间顺序',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID，数据隔离',
  `role` VARCHAR(10) NOT NULL COMMENT '角色：user/assistant，故意和DeepSeek API的role同名同值，查出来直接当上下文用',
  `content` TEXT NOT NULL COMMENT '消息正文',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`, `id`)   -- 按user_id过滤+按id倒序取最近N条
) COMMENT='AI助手对话记录表（V3）';

-- 预置记账分类种子数据，type：1=支出 2=收入
INSERT INTO `finance_categories` (`name`, `type`, `icon`) VALUES
  ('餐饮', 1, 'food'),
  ('交通', 1, 'traffic'),
  ('购物', 1, 'shopping'),
  ('娱乐', 1, 'entertainment'),
  ('居住', 1, 'home'),
  ('医疗', 1, 'medical'),
  ('其他支出', 1, 'other'),
  ('工资', 2, 'salary'),
  ('兼职', 2, 'parttime'),
  ('理财收益', 2, 'invest'),
  ('其他收入', 2, 'other');

-- ============ 公开帖子模块（V5）4 张表 ============
-- 列名严格对齐 Java 实体字段（MyBatis-Plus 驼峰转下划线），对不上就会报"Unknown column"

-- 帖子主表
CREATE TABLE `posts` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '帖子ID，主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '作者用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '正文，可空（允许纯图片帖）',
  -- ⚠ 写 TINYINT 不要写 TINYINT(1)：MySQL 驱动默认开着 tinyInt1isBit，
  --   会把 TINYINT(1) 当成 Boolean 返回、TINYINT 当成 Integer 返回，两者取值方式不同。
  --   真实库里这一列是 TINYINT，这里必须跟着写 TINYINT，否则新环境建出来的表和老库行为不一致
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1=公开 0=仅自己可见（草稿）。对应实体 isPublic',
  -- ⚠ 本表原来用的是 create_time / update_time，后来统一改成了 created_at / updated_at，
  --   和其它表保持一致。改名那次如果只改了实体没改表（或反过来），
  --   就会报 Unknown column——这类"改了一半"是最难查的
  `created_at` DATETIME NOT NULL COMMENT '发布时间',
  -- 也从原来的 DEFAULT NULL 改成了 NOT NULL：发帖时代码会直接写入当前时间，不再留空
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  `video_url` VARCHAR(500) DEFAULT NULL COMMENT '视频URL，一条帖子最多一个视频，和图片二选一',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),                     -- 查某用户的帖子（作者页）
  KEY `idx_public` (`is_public`, `created_at`)    -- 公开列表：按 is_public 过滤 + 按时间倒序
) COMMENT='公开帖子主表（V5）';

-- 帖子图片表（一对多：一帖多图，一张图一行）
CREATE TABLE `post_images` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '图片记录ID，主键',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '所属帖子ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片可访问URL（存MinIO返回的完整地址）',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序，发帖时按数组下标写入，取封面=sort最小的一张',
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`)                           -- 按帖子查它的所有图片
) COMMENT='帖子图片表（V5，一对多）';

-- 评论表
CREATE TABLE `comments` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '评论ID，主键',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论者用户ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间。本表用 created_at',
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`)                           -- 按帖子查它的所有评论 + 数评论数
) COMMENT='帖子评论表（V5）';

-- 点赞表
CREATE TABLE `post_likes` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '点赞记录ID，主键',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间。本表用 created_at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)     -- ⭐ 唯一索引：同一人对同一帖只能点一次，数据库层面防重复点赞
) COMMENT='帖子点赞表（V5）';

-- ============ 忘记密码（V6）============
-- 为什么要单独建表、不在 users 上加两个字段：重置令牌是"一次性、会过期、可能同时存在多条"的东西，
-- 塞进 users 会让用户主表被高频写；单独一张表还能保留历史记录用于排查异常重置
CREATE TABLE `password_reset_tokens` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '要重置密码的用户',
  -- ⭐ 存哈希不存明文：发给用户邮箱的是明文 token，数据库里只留 SHA-256。
  --    这样即使库被脱了，攻击者也没法反推出能用的 token 去改别人密码
  `token_hash` CHAR(64) NOT NULL COMMENT '重置令牌的SHA-256哈希——绝不存明文token，泄库也没法反推',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间，建议 30 分钟',
  -- ⭐ 用过就置 1：没有这个字段的话，同一个链接能被反复使用（重放攻击）
  `used` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已用：用一次即作废，防重放',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_token_hash` (`token_hash`),   -- 用户点链接过来，是拿 token 的哈希反查记录
  KEY `idx_user` (`user_id`)
) COMMENT='密码重置令牌表（V6）';

-- ============ 第三方登录（V7）============
-- 为什么不在 users 表加 github_id 字段：以后要接 Gitee、Google、微信，每接一个就要加一列，
-- 而且一个用户可能同时绑多个平台。做成独立的绑定表，加平台只是多插一行数据，表结构不用动
CREATE TABLE `user_oauth_binding` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '绑定到你系统里的哪个用户',
  `provider` VARCHAR(20) NOT NULL COMMENT '平台标识：github / gitee / google...',
  -- ⭐ 存平台的数字 id，不要存用户名：用户名随时能改，改了就对不上；数字 id 是永久不变的
  `provider_uid` VARCHAR(64) NOT NULL COMMENT '该平台里这个用户的唯一ID（GitHub的数字id，不是用户名——用户名可改，id不变）',
  `created_at` DATETIME NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  -- ⭐ 联合唯一索引：OAuth 回调回来时，靠 (provider, provider_uid) 查有没有绑过，
  --    查到=老用户直接登录，查不到=新用户先建账号再绑定。
  --    同时它也从数据库层面保证同一个 GitHub 账号不会被绑到两个本地账号上
  UNIQUE KEY `uk_provider_uid` (`provider`, `provider_uid`),
  KEY `idx_user` (`user_id`)             -- 反向查：某用户绑了哪些平台（解绑/展示用）
) COMMENT='第三方登录绑定表（V7）';
