package com.xxx.proj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品分类（如：手机、耳机、笔记本） */
    private String category;

    /** 品牌 */
    private String brand;

    /** 价格 */
    private BigDecimal price;

    /** 平台（淘宝/京东/拼多多） */
    private String platform;

    /** 评分（1-5） */
    private Double rating;

    /** 销量 */
    private Integer sales;

    /** 库存 */
    private Integer stock;

    /** 商品描述 */
    private String description;

    /** 商品链接 */
    private String url;
}
