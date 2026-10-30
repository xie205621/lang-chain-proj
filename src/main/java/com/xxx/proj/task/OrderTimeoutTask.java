package com.xxx.proj.task;

import com.xxx.proj.entity.Order;
import com.xxx.proj.entity.Product;
import com.xxx.proj.mapper.OrderMapper;
import com.xxx.proj.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderTimeoutTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 精准定时：等待1分钟后检查订单状态，若仍为 currentStatus 则变更为 targetStatus
     * @param orderNo      订单号
     * @param currentStatus 当前期望状态（超时未变则触发）
     * @param targetStatus  目标状态
     */
    @Async
    public void scheduleOrderTimeout(String orderNo, String currentStatus, String targetStatus) {
        try {
            Thread.sleep(60000); // 精准1分钟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) return;
        if (!currentStatus.equals(order.getStatus())) return;

        // 更新状态
        orderMapper.updateStatus(orderNo, currentStatus, targetStatus);

        // 取消 → 恢复库存
        if ("已取消".equals(targetStatus)) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + order.getQuantity());
                productMapper.updateById(product);
            }
            System.out.println("⏰ 订单 " + orderNo + " 超时未付款，已自动取消，库存已恢复");
        } else if ("已发货".equals(targetStatus)) {
            System.out.println("✅ 订单 " + orderNo + " 已自动发货");
        }
    }
}
