-- ============================================
-- 小智智能电商AI导购助手 - 完整数据库初始化
-- 在 Navicat 中直接运行此文件即可，无需手动建库
-- ============================================

-- 1. 建库
CREATE DATABASE IF NOT EXISTS `guiguxiaozhi`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `guiguxiaozhi`;

-- 2. 删旧表
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `appointment`;

-- ============================================
-- 3. 商品表
-- ============================================
CREATE TABLE `product` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name`        VARCHAR(200)  NOT NULL              COMMENT '商品名称',
    `category`    VARCHAR(50)   NOT NULL              COMMENT '商品分类',
    `brand`       VARCHAR(50)   NOT NULL              COMMENT '品牌',
    `price`       DECIMAL(10,2) NOT NULL              COMMENT '价格',
    `platform`    VARCHAR(20)   NOT NULL              COMMENT '平台（淘宝/京东/拼多多）',
    `rating`      DECIMAL(2,1)  DEFAULT 5.0           COMMENT '评分（1-5）',
    `sales`       INT           DEFAULT 0             COMMENT '销量',
    `stock`       INT           DEFAULT 0             COMMENT '库存',
    `description` VARCHAR(500)  DEFAULT ''            COMMENT '商品描述',
    `url`         VARCHAR(500)  DEFAULT ''            COMMENT '商品链接',
    PRIMARY KEY (`id`),
    INDEX `idx_category`  (`category`),
    INDEX `idx_brand`     (`brand`),
    INDEX `idx_price`     (`price`),
    INDEX `idx_platform`  (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================
-- 4. 订单表
-- ============================================
CREATE TABLE `orders` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`     VARCHAR(20)   NOT NULL              COMMENT '订单编号',
    `user_id`      BIGINT        NOT NULL              COMMENT '用户ID',
    `product_id`   BIGINT        NOT NULL              COMMENT '商品ID',
    `product_name` VARCHAR(200)  NOT NULL              COMMENT '商品名称',
    `price`        DECIMAL(10,2) NOT NULL              COMMENT '单价',
    `quantity`     INT           DEFAULT 1             COMMENT '数量',
    `total_amount` DECIMAL(10,2) NOT NULL              COMMENT '总金额',
    `address`      VARCHAR(500)  NOT NULL              COMMENT '收货地址',
    `phone`        VARCHAR(20)   NOT NULL              COMMENT '联系电话',
    `status`       VARCHAR(20)   DEFAULT '待付款'       COMMENT '订单状态：待付款/已付款/已发货/已完成/已取消',
    `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no`  (`order_no`),
    INDEX `idx_user_id`       (`user_id`),
    INDEX `idx_status`        (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================
-- 5. 模拟数据（20条商品）
-- ============================================

-- 手机（5条）
INSERT INTO `product` (`name`, `category`, `brand`, `price`, `platform`, `rating`, `sales`, `stock`, `description`) VALUES
('iPhone 15 128GB',        '手机', '苹果',   5999.00, '京东',   4.8, 15200, 500, 'A16芯片，4800万像素主摄，灵动岛设计'),
('华为Mate 60 Pro',        '手机', '华为',   6999.00, '淘宝',   4.9, 12300, 320, '麒麟9000S，卫星通话，XMAGE影像'),
('小米14 Pro',             '手机', '小米',   4999.00, '拼多多', 4.7,  9800, 450, '骁龙8Gen3，徕卡光学镜头，120W快充'),
('三星Galaxy S24 Ultra',   '手机', '三星',   9699.00, '京东',   4.6,  6700, 200, '骁龙8Gen3，S Pen，2亿像素，钛金属边框'),
('OPPO Find X7 Ultra',     '手机', 'OPPO',   5999.00, '淘宝',   4.7,  5400, 380, '双潜望长焦，哈苏影像，1英寸大底'),

-- 笔记本（5条）
('MacBook Air M3',           '笔记本', '苹果', 8999.00, '京东',   4.9, 11200, 300, 'M3芯片，15.3寸Liquid Retina，18小时续航'),
('联想ThinkPad X1 Carbon',   '笔记本', '联想', 9999.00, '淘宝',   4.8,  7800, 180, 'Ultra 7处理器，14寸2.8K OLED，轻至1.11kg'),
('华为MateBook X Pro',       '笔记本', '华为', 7999.00, '京东',   4.8,  6500, 250, 'Ultra 9处理器，14.2寸OLED原色屏，超级终端'),
('小米RedmiBook Pro 16',     '笔记本', '小米', 4999.00, '拼多多', 4.6,  9200, 420, 'i7-13700H，16寸2.5K 120Hz屏，72Wh大电池'),
('华硕灵耀14 2024',          '笔记本', '华硕', 5999.00, '淘宝',   4.5,  4300, 310, 'Ultra 7，14寸2.8K OLED，75Wh电池，军规认证'),

-- 蓝牙耳机（5条）
('AirPods Pro 2',            '耳机', '苹果',   1899.00, '京东',   4.8, 25000, 600, 'H2芯片，自适应降噪，个性化空间音频'),
('华为FreeBuds Pro 3',       '耳机', '华为',   1299.00, '淘宝',   4.7, 14300, 480, '星闪连接，智慧动态降噪，高清空间音频'),
('小米Buds 4 Pro',           '耳机', '小米',    899.00, '拼多多', 4.5, 11200, 550, 'LHDC 5.0，48dB深度降噪，38小时续航'),
('索尼WF-1000XM5',           '耳机', '索尼',   1999.00, '京东',   4.9,  9800, 280, '业界顶级降噪，Hi-Res Audio，8小时续航'),
('漫步者NeoBuds Pro 2',      '耳机', '漫步者',  599.00, '淘宝',   4.4,  6300, 700, 'Hi-Res双金标，数字分频，空间音频，80ms低延迟'),

-- 键盘鼠标（5条）
('罗技MX Keys S键盘',        '键盘', '罗技',   799.00, '京东',   4.8,  8700, 350, '智能背光，多设备无缝切换，静音按键，Flow跨屏'),
('Cherry MX 3.0S机械键盘',   '键盘', 'Cherry', 699.00, '淘宝',   4.7,  5200, 260, '原厂Cherry轴，全键无冲，铝合金面板，RGB灯效'),
('罗技MX Master 3S鼠标',     '鼠标', '罗技',   699.00, '京东',   4.9, 15000, 400, 'MagSpeed电磁滚轮，8K DPI，静音按键，Flow跨屏'),
('雷蛇DeathAdder V3 Pro',    '鼠标', '雷蛇',   999.00, '淘宝',   4.7,  7200, 310, '63g超轻量化，Focus Pro 30K传感器，90小时续航'),
('VGN V98 Pro机械键盘',      '键盘', 'VGN',    299.00, '拼多多', 4.5,  9800, 500, 'Gasket结构，三模连接，8000mAh电池，PBT键帽');

-- ============================================
-- 完成！
-- ============================================
SELECT '✅ 数据库初始化完成！' AS result;
SELECT COUNT(*) AS 'product表数据条数' FROM `product`;
