package com.xxx.proj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.proj.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 关键词搜索：在名称、分类、品牌、描述中匹配
     */
    @Select("SELECT * FROM product WHERE name LIKE CONCAT('%',#{keyword},'%') " +
            "OR category LIKE CONCAT('%',#{keyword},'%') " +
            "OR brand LIKE CONCAT('%',#{keyword},'%') " +
            "OR description LIKE CONCAT('%',#{keyword},'%') " +
            "ORDER BY sales DESC")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 按分类和价格范围搜索
     */
    @Select("<script>" +
            "SELECT * FROM product WHERE 1=1 " +
            "<if test='category != null and category != \"\"'>AND category = #{category}</if> " +
            "<if test='brand != null and brand != \"\"'>AND brand = #{brand}</if> " +
            "<if test='minPrice != null'>AND price &gt;= #{minPrice}</if> " +
            "<if test='maxPrice != null'>AND price &lt;= #{maxPrice}</if> " +
            "ORDER BY sales DESC" +
            "</script>")
    List<Product> searchByFilter(@Param("category") String category,
                                 @Param("brand") String brand,
                                 @Param("minPrice") java.math.BigDecimal minPrice,
                                 @Param("maxPrice") java.math.BigDecimal maxPrice);

    /**
     * 按平台和商品名称查询，用于比价
     */
    @Select("SELECT * FROM product WHERE name LIKE CONCAT('%',#{productName},'%') " +
            "ORDER BY price ASC")
    List<Product> compareByProductName(@Param("productName") String productName);

    /**
     * 获取热门推荐（按销量+评分排序）
     */
    @Select("SELECT * FROM product ORDER BY (sales * rating) DESC LIMIT #{limit}")
    List<Product> getTopRecommended(@Param("limit") int limit);

    /**
     * 按分类推荐
     */
    @Select("SELECT * FROM product WHERE category = #{category} " +
            "ORDER BY (sales * rating) DESC LIMIT #{limit}")
    List<Product> recommendByCategory(@Param("category") String category,
                                       @Param("limit") int limit);

    /**
     * 降级搜索：放宽预算至 maxPrice * 1.2，按价格升序，最多返回 limit 条
     * 用于严格搜索无结果时的相近价位推荐
     */
    @Select("<script>" +
            "SELECT * FROM product WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (name LIKE CONCAT('%',#{keyword},'%') " +
            "OR category LIKE CONCAT('%',#{keyword},'%') " +
            "OR brand LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='category != null and category != \"\"'>AND category = #{category}</if> " +
            "<if test='brand != null and brand != \"\"'>AND brand = #{brand}</if> " +
            "AND price &lt;= #{relaxedMaxPrice} " +
            "ORDER BY price ASC LIMIT #{limit}" +
            "</script>")
    List<Product> searchRelaxed(@Param("keyword") String keyword,
                                @Param("category") String category,
                                @Param("brand") String brand,
                                @Param("relaxedMaxPrice") java.math.BigDecimal relaxedMaxPrice,
                                @Param("limit") int limit);

    /**
     * 最宽搜索：不限制价格，按价格升序取最便宜的 limit 条
     * 用于预算差距较大时的参考推荐
     */
    @Select("<script>" +
            "SELECT * FROM product WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (name LIKE CONCAT('%',#{keyword},'%') " +
            "OR category LIKE CONCAT('%',#{keyword},'%') " +
            "OR brand LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='category != null and category != \"\"'>AND category = #{category}</if> " +
            "<if test='brand != null and brand != \"\"'>AND brand = #{brand}</if> " +
            "ORDER BY price ASC LIMIT #{limit}" +
            "</script>")
    List<Product> searchCheapest(@Param("keyword") String keyword,
                                  @Param("category") String category,
                                  @Param("brand") String brand,
                                  @Param("limit") int limit);
}
