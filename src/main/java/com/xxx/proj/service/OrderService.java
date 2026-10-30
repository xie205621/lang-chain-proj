package com.xxx.proj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.proj.entity.Order;

import java.util.List;

public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    Order createOrder(Long userId, Long productId, String productName,
                      java.math.BigDecimal price, Integer quantity,
                      String address, String phone, String recipient);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> queryByUserId(Long userId);

    /**
     * 根据订单编号查询订单
     */
    Order queryByOrderNo(String orderNo);

    /**
     * 取消订单
     */
    boolean cancelOrder(String orderNo);

    /**
     * 模拟付款：将订单状态从"待付款"更新为"已付款"
     * @param orderNo 订单编号
     * @return 操作结果描述
     */
    String confirmPayment(String orderNo);

    /**
     * 确认收货：将订单状态从"已发货"更新为"已完成"
     */
    String confirmReceipt(String orderNo);

    /**
     * 退货：将订单状态从"已发货"更新为"已退货"，恢复库存和销量
     */
    String returnOrder(String orderNo);

    /**
     * 按状态查询订单
     */
    List<Order> queryByUserIdAndStatus(Long userId, String status);
}
