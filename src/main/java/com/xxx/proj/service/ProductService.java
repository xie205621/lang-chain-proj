package com.xxx.proj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.proj.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService extends IService<Product> {

    /**
     * 关键词搜索商品
     */
    List<Product> searchByKeyword(String keyword);

    /**
     * 多条件筛选商品
     */
    List<Product> searchByFilter(String category, String brand, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 多平台比价
     */
    List<Product> comparePrice(String productName);

    /**
     * 获取热门推荐Top N
     */
    List<Product> getTopRecommended(int limit);

    /**
     * 按分类推荐
     */
    List<Product> recommendByCategory(String category, int limit);

    /**
     * 生成推荐理由
     */
    String generateRecommendReason(Product product);

    /**
     * 生成推荐理由（感知预算）
     * @param product 商品
     * @param budget 用户预算上限，null表示无预算限制
     */
    String generateRecommendReason(Product product, java.math.BigDecimal budget);

    /**
     * 降级搜索：严格→放宽20%→最低价Top3
     * @return SearchResult 包含商品列表和搜索类型标记
     */
    SearchResult searchWithFallback(String keyword, String category, String brand,
                                    java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    /**
     * 搜索结果封装
     */
    class SearchResult {
        private List<Product> products;
        private String type; // "strict" / "relaxed" / "cheapest" / "empty"

        public SearchResult(List<Product> products, String type) {
            this.products = products;
            this.type = type;
        }

        public List<Product> getProducts() { return products; }
        public String getType() { return type; }
        public boolean isEmpty() { return products == null || products.isEmpty(); }
    }
}
