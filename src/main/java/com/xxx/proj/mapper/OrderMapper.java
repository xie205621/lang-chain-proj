package com.xxx.proj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.proj.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据用户ID查询订单列表
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    /**
     * 根据订单编号查询
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Order findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据状态查询订单
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status = #{status} ORDER BY update_time DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId,
                                       @Param("status") String status);

    /**
     * 直接 SQL 更新订单状态，绕过 MyBatis-Plus 封装，确保数据库一定写入
     * @return 受影响行数（1=成功，0=未匹配到符合条件的订单）
     */
    @Update("UPDATE orders SET status = #{newStatus}, update_time = NOW() WHERE order_no = #{orderNo} AND status = #{oldStatus}")
    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("oldStatus") String oldStatus,
                     @Param("newStatus") String newStatus);
}
    