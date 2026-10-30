package com.xxx.proj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 收货地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 收货人姓名 */
    private String recipient;

    /** 订单状态：待付款/已付款/已发货/已完成/已取消/已退货 */
    private String status;

    /** 创建时间（仅内存使用，用于自动发货/取消计时，不映射数据库） */
    @TableField(exist = false)
    private LocalDateTime createdAt;

    /** 更新时间（每次状态变更时更新） */
    private LocalDateTime updateTime;
}
