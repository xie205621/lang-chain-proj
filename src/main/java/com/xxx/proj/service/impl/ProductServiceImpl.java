package com.xxx.proj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.proj.entity.Product;
import com.xxx.proj.mapper.ProductMapper;
import com.xxx.proj.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Product> searchByKeyword(String keyword) {
        return productMapper.searchByKeyword(keyword);
    }

    @Override
    public List<Product> searchByFilter(String category, String brand, BigDecimal minPrice, BigDecimal maxPrice) {
        return productMapper.searchByFilter(category, brand, minPrice, maxPrice);
    }

    @Override
    public List<Product> comparePrice(String productName) {
        return productMapper.compareByProductName(productName);
    }

    @Override
    public List<Product> getTopRecommended(int limit) {
        return productMapper.getTopRecommended(limit);
    }

    @Override
    public List<Product> recommendByCategory(String category, int limit) {
        return productMapper.recommendByCategory(category, limit);
    }

    @Override
    public SearchResult searchWithFallback(String keyword, String category, String brand,
                                           BigDecimal minPrice, BigDecimal maxPrice) {
        // 第一步：严格搜索
        List<Product> strictResults;
        if (keyword != null && !keyword.isEmpty()) {
            // 关键词搜索，然后在 Java 侧按价格过滤
            List<Product> keywordResults = productMapper.searchByKeyword(keyword);
            strictResults = filterByPrice(keywordResults, minPrice, maxPrice);
        } else {
            strictResults = productMapper.searchByFilter(category, brand, minPrice, maxPrice);
        }

        if (!strictResults.isEmpty()) {
            return new SearchResult(strictResults, "strict");
        }

        // 第二步：如果提供了 maxPrice，放宽 20% 预算
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal relaxedMax = maxPrice.multiply(new BigDecimal("1.2")).setScale(2, RoundingMode.HALF_UP);
            List<Product> relaxedResults = productMapper.searchRelaxed(keyword, category, brand, relaxedMax, 3);
            if (!relaxedResults.isEmpty()) {
                return new SearchResult(relaxedResults, "relaxed");
            }
        }

        // 第三步：完全不限价格，取最便宜的 3 个
        // 但需要判断是否远超预算（超过 maxPrice * 1.2），如果是则标记为 "far_above"
        List<Product> cheapestResults = productMapper.searchCheapest(keyword, category, brand, 3);
        if (!cheapestResults.isEmpty()) {
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal farAboveThreshold = maxPrice.multiply(new BigDecimal("1.2")).setScale(2, RoundingMode.HALF_UP);
                // 如果最便宜的商品都远超 1.2 倍预算，标记为 "far_above"，不推荐
                if (cheapestResults.get(0).getPrice().compareTo(farAboveThreshold) > 0) {
                    return new SearchResult(cheapestResults, "far_above");
                }
            }
            return new SearchResult(cheapestResults, "cheapest");
        }

        return new SearchResult(Collections.emptyList(), "empty");
    }

    /**
     * 在 Java 侧按价格范围过滤商品
     */
    private List<Product> filterByPrice(List<Product> products, BigDecimal minPrice, BigDecimal maxPrice) {
        if (products == null || products.isEmpty()) return Collections.emptyList();
        return products.stream()
                .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String generateRecommendReason(Product product) {
        return generateRecommendReason(product, null);
    }

    @Override
    public String generateRecommendReason(Product product, BigDecimal budget) {
        StringBuilder reason = new StringBuilder();
        reason.append("推荐理由：");

        if (product.getRating() != null && product.getRating() >= 4.5) {
            reason.append("⭐ 评分 ").append(product.getRating()).append(" 分，用户口碑极佳；");
        }
        if (product.getSales() != null && product.getSales() >= 1000) {
            reason.append("🔥 累计销量 ").append(product.getSales()).append("+，市场热销爆款；");
        }
        if (product.getRating() != null && product.getSales() != null) {
            double score = product.getRating() * Math.log10(product.getSales() + 1);
            if (score > 10) {
                reason.append("🏆 综合热度指数极高，性价比突出；");
            }
        }
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            reason.append("📝 ").append(product.getDescription()).append("；");
        }

        reason.append("💰 仅售 ¥").append(product.getPrice());

        // 预算感知：标注是否超预算
        if (budget != null && product.getPrice().compareTo(budget) > 0) {
            reason.append("（⚠️ 该商品略高于你的预算 ¥").append(budget).append("，属于相近价位替代选择）");
        }

        reason.append("。");
        return reason.toString();
    }
}
