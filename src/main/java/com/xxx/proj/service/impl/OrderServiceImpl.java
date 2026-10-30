package com.xxx.proj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.proj.entity.Order;
import com.xxx.proj.entity.Product;
import com.xxx.proj.mapper.OrderMapper;
import com.xxx.proj.mapper.ProductMapper;
import com.xxx.proj.service.OrderService;
import com.xxx.proj.task.OrderTimeoutTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderTimeoutTask orderTimeoutTask;

    @Override
    @Transactional
    public Order createOrder(Long userId, Long productId, String productName,
                             BigDecimal price, Integer quantity,
                             String address, String phone, String recipient) {
        // 扣减库存
        Product product = productMapper.selectById(productId);
        if (product != null) {
            int newStock = product.getStock() - quantity;
            product.setStock(newStock);
            productMapper.updateById(product);
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(1L);  // 固定为1
        order.setProductId(productId);
        order.setProductName(productName);
        order.setPrice(price);
        order.setQuantity(quantity != null ? quantity : 1);
        // 计算满减后金额
        BigDecimal originalTotal = price.multiply(BigDecimal.valueOf(order.getQuantity()));
        order.setTotalAmount(applyDiscount(originalTotal));
        order.setAddress(address);
        order.setPhone(phone);
        order.setRecipient(recipient);
        order.setStatus("待付款");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        save(order);

        // 下单后1分钟若未付款，自动取消
        orderTimeoutTask.scheduleOrderTimeout(order.getOrderNo(), "待付款", "已取消");

        return order;
    }

    @Override
    public List<Order> queryByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Override
    public Order queryByOrderNo(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        // 自动发货/取消已由 OrderTimeoutTask 异步接管，查询时无需再处理
        return order;
    }

    @Override
    @Transactional
    public boolean cancelOrder(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo.trim());
        Order order = this.getOne(queryWrapper);
        if (order == null) {
            return false;
        }
        // 只有待付款和已付款状态可以取消
        if ("待付款".equals(order.getStatus()) || "已付款".equals(order.getStatus())) {
            LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Order::getOrderNo, orderNo.trim())
                         .in(Order::getStatus, "待付款", "已付款")
                         .set(Order::getStatus, "已取消")
                         .set(Order::getUpdateTime, LocalDateTime.now());
            boolean updated = this.update(updateWrapper);
            if (updated) {
                // 取消订单时恢复库存
                Product product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + order.getQuantity());

                    // 如果订单已付款，销量已经加过，取消时要减回去
                    if ("已付款".equals(order.getStatus())) {
                        product.setSales(product.getSales() - order.getQuantity());
                        // 防止销量变成负数（安全保护）
                        if (product.getSales() < 0) {
                            product.setSales(0);
                        }
                    }

                    productMapper.updateById(product);
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    @Transactional
    public String confirmPayment(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return "🤔 请提供订单号。";
        }

        // 先查订单确认状态
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo.trim());
        Order order = this.getOne(queryWrapper);

        if (order == null) {
            return "😔 未查询到该订单，请核实订单号是否正确。";
        }

        String status = order.getStatus();

        if ("待付款".equals(status)) {
            // 直接 SQL 更新，绕过 MyBatis-Plus 封装，确保数据库一定写入
            int rows = orderMapper.updateStatus(orderNo.trim(), "待付款", "已付款");

            if (rows > 0) {
                // 付款成功，增加商品销量
                Product product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    product.setSales(product.getSales() + order.getQuantity());
                    productMapper.updateById(product);
                }

                // 付款后1分钟自动发货
                orderTimeoutTask.scheduleOrderTimeout(orderNo.trim(), "已付款", "已发货");

                return "✅ 模拟付款成功！\n\n" +
                       "📦 订单编号：" + order.getOrderNo() + "\n" +
                       "🛍 商品：" + order.getProductName() + "\n" +
                       "💰 金额：¥" + order.getTotalAmount() + "\n" +
                       "📌 当前状态：**已付款**\n\n" +
                       "💡 说明：当前系统为课程演示版，未接入真实支付平台，本次付款为系统内状态流转。";
            } else {
                return "😔 模拟付款失败，订单状态更新未成功，请稍后重试。";
            }
        }

        if ("已付款".equals(status)) {
            return "⚠️ 订单「" + orderNo + "」已经是「已付款」状态，无需重复付款。";
        }

        if ("已取消".equals(status)) {
            return "⚠️ 订单「" + orderNo + "」已被取消，无法付款。";
        }

        if ("已发货".equals(status) || "已完成".equals(status)) {
            return "⚠️ 订单「" + orderNo + "」当前状态为「" + status + "」，无需再次付款。";
        }

        return "⚠️ 订单「" + orderNo + "」当前状态为「" + status + "」，暂不支持付款操作。";
    }

    @Override
    @Transactional
    public String confirmReceipt(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return "🤔 请提供订单号。";
        }
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo.trim());
        Order order = this.getOne(queryWrapper);

        if (order == null) {
            return "😔 未查询到该订单。";
        }

        if ("已发货".equals(order.getStatus())) {
            int rows = orderMapper.updateStatus(orderNo.trim(), "已发货", "已完成");
            if (rows > 0) {
                return "✅ 确认收货成功！订单「" + orderNo + "」状态已更新为「已完成」。\n期待您再次光临！🛍️";
            }
            return "😔 确认收货失败，请稍后重试。";
        }

        if ("已完成".equals(order.getStatus())) {
            return "⚠️ 订单已完成，无需重复确认收货。";
        }

        return "⚠️ 订单当前状态为「" + order.getStatus() + "」，仅「已发货」状态可确认收货。";
    }

    @Override
    @Transactional
    public String returnOrder(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return "🤔 请提供订单号。";
        }
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo.trim());
        Order order = this.getOne(queryWrapper);

        if (order == null) {
            return "😔 未查询到该订单。";
        }

        if ("已发货".equals(order.getStatus())) {
            int rows = orderMapper.updateStatus(orderNo.trim(), "已发货", "已退货");
            if (rows > 0) {
                // 退货恢复库存和销量
                Product product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + order.getQuantity());
                    product.setSales(product.getSales() - order.getQuantity());
                    if (product.getSales() < 0) product.setSales(0);
                    productMapper.updateById(product);
                }
                return "✅ 退货成功！订单「" + orderNo + "」已更新为「已退货」。\n库存和销量已恢复。";
            }
            return "😔 退货处理失败，请稍后重试。";
        }

        if ("已退货".equals(order.getStatus())) {
            return "⚠️ 订单已退货，无需重复操作。";
        }

        if ("已完成".equals(order.getStatus())) {
            return "⚠️ 订单已完成，无法退货。只有「已发货」状态的订单可以申请退货。";
        }

        return "⚠️ 订单当前状态为「" + order.getStatus() + "」，仅「已发货」状态可退货。";
    }

    @Override
    public List<Order> queryByUserIdAndStatus(Long userId, String status) {
        return orderMapper.findByUserIdAndStatus(userId, status);
    }

    /**
     * 生成订单编号：yyyyMMddHHmmss + 4位随机数
     */
    /**
     * 满减规则：满199-30 / 满499-80 / 满999-200 / 满1999-500
     */
    private BigDecimal applyDiscount(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1999")) >= 0) {
            return amount.subtract(new BigDecimal("500"));
        } else if (amount.compareTo(new BigDecimal("999")) >= 0) {
            return amount.subtract(new BigDecimal("200"));
        } else if (amount.compareTo(new BigDecimal("499")) >= 0) {
            return amount.subtract(new BigDecimal("80"));
        } else if (amount.compareTo(new BigDecimal("199")) >= 0) {
            return amount.subtract(new BigDecimal("30"));
        }
        return amount;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return timestamp + random;
    }
}
