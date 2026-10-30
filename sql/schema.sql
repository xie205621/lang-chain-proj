-- ============================================
-- 小智智能电商AI导购助手 - 建表脚本
-- ============================================

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `appointment`;

-- ============================================
-- 商品表
-- ============================================
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `category` VARCHAR(50) NOT NULL COMMENT '商品分类',
    `brand` VARCHAR(50) NOT NULL COMMENT '品牌',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `platform` VARCHAR(20) NOT NULL COMMENT '平台（淘宝/京东/拼多多）',
    `rating` DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分（1-5）',
    `sales` INT DEFAULT 0 COMMENT '销量',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `description` VARCHAR(500) DEFAULT '' COMMENT '商品描述',
    `url` VARCHAR(500) DEFAULT '' COMMENT '商品链接',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_brand` (`brand`),
    INDEX `idx_price` (`price`),
    INDEX `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================
-- 订单表
-- ============================================
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(20) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `address` VARCHAR(500) NOT NULL COMMENT '收货地址',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `status` VARCHAR(20) DEFAULT '待付款' COMMENT '订单状态：待付款/已付款/已发货/已完成/已取消',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
