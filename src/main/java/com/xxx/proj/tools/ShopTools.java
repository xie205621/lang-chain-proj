package com.xxx.proj.tools;

import com.xxx.proj.entity.Order;
import com.xxx.proj.entity.Product;
import com.xxx.proj.service.OrderService;
import com.xxx.proj.service.ProductService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShopTools {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    // ==================== 1. 商品搜索 ====================

    @Tool(name = "searchProduct",
            value = "根据用户提供的关键词、品类、品牌、价格范围等条件搜索商品。" +
                    "如果用户提到了具体的商品名称，请用keyword参数传入；" +
                    "如果用户提到了品类（如'手机''耳机'），请用category参数传入；" +
                    "如果用户提到了品牌，请用brand参数传入；" +
                    "如果用户提到了价格范围（如'300以内''1000-2000'），请用minPrice和maxPrice参数传入。" +
                    "搜索结果全部来自数据库，请如实返回，不要编造任何商品信息。")
    public String searchProduct(
            @P(value = "搜索关键词，如商品名称", required = false) String keyword,
            @P(value = "商品分类，如：手机、耳机、笔记本、键盘、鼠标", required = false) String category,
            @P(value = "品牌名称，如：华为、苹果、小米、索尼", required = false) String brand,
            @P(value = "最低价格", required = false) BigDecimal minPrice,
            @P(value = "最高价格", required = false) BigDecimal maxPrice) {

        ProductService.SearchResult result = productService.searchWithFallback(
                keyword, category, brand, minPrice, maxPrice);

        switch (result.getType()) {
            case "strict":
                return formatProductList("🔍 为您找到以下符合条件的商品：", result.getProducts());

            case "relaxed":
                return "⚠️ 暂无完全符合预算的商品，但找到以下**相近价位商品**（略超预算）：\n\n" +
                        formatProductListBody(result.getProducts()) +
                        "\n💡 这些商品价格略高于您的预算，您可以考虑适当提高预算或调整筛选条件。";

            case "cheapest":
                return "😔 当前商品库暂无该价位商品，以下是同品类**价格最低的参考商品**（与您的预算差距较大）：\n\n" +
                        formatProductListBody(result.getProducts()) +
                        "\n💡 建议您提高预算或尝试其他品类/品牌。";

            case "far_above":
                // 最便宜的商品都远超预算（>1.2倍），不展示商品列表，只说明情况
                Product cheapest = result.getProducts().get(0);
                String categoryName = (category != null && !category.isEmpty()) ? category : "该品类";
                return "😔 当前商品库中" + categoryName + "最低价为 **¥" + cheapest.getPrice() +
                        "**（" + cheapest.getName() + "），明显高于您的预算。\n\n" +
                        "💡 建议您：\n" +
                        "1. 适当提高预算\n" +
                        "2. 尝试其他品牌或品类\n" +
                        "3. 如果预算必须控制在此范围，可以考虑键盘、鼠标、入门耳机等其他数码配件";

            default:
                return "😔 当前商品库暂无相关商品，请尝试更换关键词、品类或提高预算。";
        }
    }

    // ==================== 2. 智能推荐 ====================

    @Tool(name = "recommendProduct",
            value = "根据用户的需求和偏好，智能推荐Top3热门商品，并为每个商品附带推荐理由。" +
                    "如果用户提到了品类（如'笔记本''耳机'），请传入category参数；" +
                    "如果用户提到了预算范围，请结合用户的预算进行推荐。" +
                    "推荐时请务必附带推荐理由，说明为什么推荐这款商品。")
    public String recommendProduct(
            @P(value = "用户想要的商品分类", required = false) String category,
            @P(value = "用户预算上限", required = false) BigDecimal budget) {

        List<Product> products;

        if (category != null && !category.isEmpty()) {
            products = productService.recommendByCategory(category, 3);
        } else {
            products = productService.getTopRecommended(3);
        }

        if (products.isEmpty()) {
            return "😔 抱歉，暂时没有找到合适的推荐商品。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 根据您的需求，为您精选推荐以下 Top3 商品：\n\n");

        String[] medals = {"🥇", "🥈", "🥉"};

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(medals[i]).append(" **").append(p.getName()).append("**\n");
            sb.append("   🆔 商品ID：").append(p.getId());
            sb.append(" | 🏷 品牌：").append(p.getBrand());
            sb.append(" | 💰 价格：¥").append(p.getPrice());
            sb.append(" | ⭐ 评分：").append(p.getRating()).append("\n");
            sb.append("   📦 平台：").append(p.getPlatform());
            sb.append(" | 📈 销量：").append(p.getSales()).append("+");
            sb.append(" | 📦 库存：").append(p.getStock()).append("件\n");
            sb.append("   💡 ").append(productService.generateRecommendReason(p, budget)).append("\n\n");
        }

        return sb.toString();
    }

    // ==================== 3. 多平台比价 ====================

    @Tool(name = "comparePrice",
            value = "根据用户提供的商品名称，查询数据库中该商品在各平台的真实价格数据。" +
                    "用户说'比价''哪个平台便宜''对比一下'时调用此工具。" +
                    "只返回数据库中实际存在的数据，不编造其他平台信息。" +
                    "请以对比表格的形式展示各平台的价格、评分、销量。" +
                    "如果只查询到一个平台的数据，如实说明，不要自行编造其他平台。")
    public String comparePrice(
            @P(value = "要比价的商品名称") String productName) {

        if (productName == null || productName.isEmpty()) {
            return "🤔 请告诉我您想比价的商品名称哦～";
        }

        List<Product> products = productService.comparePrice(productName);

        if (products.isEmpty()) {
            return "😔 抱歉，各平台暂未找到「" + productName + "」的相关商品信息。";
        }

        // 统计平台
        java.util.Set<String> platforms = products.stream()
                .map(Product::getPlatform)
                .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        sb.append("📊 「**").append(productName).append("**」价格对比：\n\n");

        // 诚实说明数据覆盖范围
        if (platforms.size() == 1) {
            sb.append("ℹ️ 当前商品库仅查询到 **").append(platforms.iterator().next())
                    .append("** 平台数据，暂无淘宝/拼多多等其他平台同款数据。\n\n");
        }

        sb.append("| 商品ID | 平台 | 价格 | 评分 | 销量 | 库存 |\n");
        sb.append("|--------|------|------|------|------|------|\n");

        BigDecimal minPrice = null;
        String cheapestPlatform = "";

        for (Product p : products) {
            sb.append("| ").append(p.getId())
                    .append(" | ").append(p.getPlatform())
                    .append(" | ¥").append(p.getPrice())
                    .append(" | ⭐").append(p.getRating())
                    .append(" | ").append(p.getSales()).append("+")
                    .append(" | ").append(p.getStock()).append("件 |\n");

            if (minPrice == null || p.getPrice().compareTo(minPrice) < 0) {
                minPrice = p.getPrice();
                cheapestPlatform = p.getPlatform();
            }
        }

        sb.append("\n🏆 **最低价在").append(cheapestPlatform)
                .append("**，仅售 ¥").append(minPrice).append("！\n");

        if (platforms.size() == 1) {
            sb.append("💡 如需更多平台比价数据，可尝试更换商品名称搜索。");
        } else {
            sb.append("💡 建议综合考虑价格、评分和平台售后保障再做决定哦～");
        }

        return sb.toString();
    }

    // ==================== 4. 一键下单 ====================

    @Tool(name = "createOrder",
            value = "为用户创建模拟订单。创建后订单状态为'待付款'。本系统为课程演示版，不生成真实付款链接。" +
                    "在调用此工具前，请务必确保已和用户确认以下信息：" +
                    "（1）商品名称和ID；（2）购买数量；（3）收货人姓名；（4）收货地址；（5）联系电话。" +
                    "如果用户未提供完整信息，请主动追问缺失的信息。信息确认完毕后再调用本工具创建订单。")
    public String createOrder(
            @P(value = "用户ID") Long userId,
            @P(value = "商品ID") Long productId,
            @P(value = "商品名称") String productName,
            @P(value = "商品单价") BigDecimal price,
            @P(value = "购买数量") Integer quantity,
            @P(value = "收货人姓名") String recipient,
            @P(value = "收货地址") String address,
            @P(value = "联系电话") String phone) {

        // 校验必填信息
        StringBuilder missing = new StringBuilder();
        if (userId == null) missing.append("用户信息、");
        if (productId == null) missing.append("商品信息、");
        if (recipient == null || recipient.isEmpty()) missing.append("收货人姓名、");
        if (address == null || address.isEmpty()) missing.append("收货地址、");
        if (phone == null || phone.isEmpty()) missing.append("联系电话、");

        if (missing.length() > 0) {
            missing.setLength(missing.length() - 1); // 去掉最后的顿号
            return "⚠️ 下单信息不完整，缺少：" + missing + "。请提供完整信息后再下单哦～";
        }

        // 查询商品确认存在
        Product product = productService.getById(productId);
        if (product == null) {
            return "😔 商品不存在或已下架，请选择其他商品。";
        }

        // 校验库存
        if (product.getStock() == null || product.getStock() <= 0) {
            return "😔 抱歉，「" + product.getName() + "」当前已售罄，暂时无货。建议您看看其他同类商品～";
        }

        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }

        if (quantity > product.getStock()) {
            return "😔 抱歉，「" + product.getName() + "」当前仅剩 " + product.getStock() + " 件库存，无法满足您的购买数量（" + quantity + " 件）。建议减少数量或选择其他商品～";
        }

        BigDecimal finalPrice = price != null ? price : product.getPrice();

        Order order = orderService.createOrder(userId, productId, productName != null ? productName : product.getName(),
                finalPrice, quantity, address, phone, recipient);

        return "✅ **订单已创建成功！** 🛒\n\n" +
                "📦 订单编号：**" + order.getOrderNo() + "**\n" +
                "🛍 商品： " + order.getProductName() + "\n" +
                "💰 单价： ¥" + order.getPrice() + "\n" +
                "🔢 数量： " + order.getQuantity() + "\n" +
                "💵 总金额： **¥" + order.getTotalAmount() + "**\n" +
                "👤 收货人： " + order.getRecipient() + "\n" +
                "📍 收货地址： " + order.getAddress() + "\n" +
                "📞 联系电话： " + order.getPhone() + "\n" +
                "📌 当前状态：**" + order.getStatus() + "**\n\n" +
                "💡 说明：当前系统为课程演示版，仅支持模拟订单流转，不提供真实付款链接。\n\n" +
                "你可以继续回复：\n" +
                "1. **确认付款** — 模拟将订单状态更新为'已付款'\n" +
                "2. **查询订单** — 查看当前订单状态\n" +
                "3. **取消订单** — 取消未发货的订单";
    }

    // ==================== 5. 模拟付款 ====================
    @Tool(name = "confirmPayment",
            value = "对订单进行模拟付款，将订单状态从'待付款'更新为'已付款'。本工具不处理真实支付，仅做系统内状态流转。" +
                    "当用户说'确认付款''模拟付款''我已付款''我要付款'时调用此工具。")
    public String confirmPayment(
            @P(value = "订单编号") String orderNo) {

        if (orderNo == null || orderNo.isEmpty()) {
            return "🤔 请提供要付款的订单编号～";
        }
        return orderService.confirmPayment(orderNo);
    }
    // ==================== 6. 查询订单 ====================
    @Tool(name = "queryOrder",
            value = "查询用户的订单信息（系统内订单状态）。本系统未接入真实物流API，不返回实时物流轨迹。" +
                    "可以通过订单编号精确查询，也可以查询用户的所有订单。" +
                    "如果用户说'查订单''我的订单''刚才的订单''物流''快递到哪了'时调用此工具。")
    public String queryOrder(
            @P(value = "订单编号", required = false) String orderNo) {
        // 固定用户ID为1
        Long userId = 1L;
        if (orderNo != null && !orderNo.isEmpty()) {
            // 按订单编号精确查询
            Order order = orderService.queryByOrderNo(orderNo);
            if (order == null) {
                return "😔 未找到订单编号为「" + orderNo + "」的订单，请核实订单号是否正确。";
            }
            return formatSingleOrder(order);
        }

        // 按固定用户ID查询所有订单
        List<Order> orders = orderService.queryByUserId(userId);
        if (orders.isEmpty()) {
            return "📭 您还没有任何订单记录，快去逛逛吧～🛒";
        }
        return formatOrderList(orders);
    }

    // ==================== 7. 取消订单 ====================

    @Tool(name = "cancelOrder",
            value = "取消用户的订单。取消前需要用户二次确认。仅允许取消'待付款'和'已付款'状态的订单，" +
                    "已发货/已完成/已取消的订单不能取消。取消操作是将状态更新为'已取消'，不会删除订单记录。" +
                    "当用户说'取消订单''不要了'时调用此工具。")
    public String cancelOrder(
            @P(value = "要取消的订单编号") String orderNo) {

        if (orderNo == null || orderNo.isEmpty()) {
            return "🤔 请提供要取消的订单编号～";
        }

        Order order = orderService.queryByOrderNo(orderNo);
        if (order == null) {
            return "😔 未找到订单编号为「" + orderNo + "」的订单，请核实后重试。";
        }

        if ("已取消".equals(order.getStatus())) {
            return "⚠️ 订单「" + orderNo + "」已经被取消过了，无需重复操作。";
        }

        if ("已发货".equals(order.getStatus())) {
            return "⚠️ 订单「" + orderNo + "」已发货，无法取消。您可以回复【退货】申请售后退货。";
        }

        if ("已完成".equals(order.getStatus()) || "已退货".equals(order.getStatus())) {
            return "⚠️ 订单「" + orderNo + "」当前状态为「" + order.getStatus() + "」，无法取消。";
        }

        boolean success = orderService.cancelOrder(orderNo);
        if (success) {
            return "✅ 订单「" + orderNo + "」已成功取消。期待您下次光临！🛍️";
        } else {
            return "😔 订单取消失败，请稍后重试或联系人工客服。";
        }
    }

    // ==================== 8. 确认收货 ====================

    @Tool(name = "confirmReceipt",
            value = "用户确认收到商品，将订单状态从【已发货】更新为【已完成】。确认后不可再退货。" +
                    "当用户说【确认收货】【收到了】【已收到】时调用此工具。" +
                    "注意：已完成订单不可退货，请提示用户确认前检查商品。")
    public String confirmReceipt(
            @P(value = "订单编号") String orderNo) {

        if (orderNo == null || orderNo.isEmpty()) {
            return "🤔 请提供要确认收货的订单编号～";
        }

        return orderService.confirmReceipt(orderNo);
    }

    // ==================== 9. 退货 ====================

    @Tool(name = "returnOrder",
            value = "对已发货的订单申请退货，将订单状态更新为【已退货】，同时恢复商品库存和销量。" +
                    "仅【已发货】状态可退货；已完成订单不可退货。" +
                    "退货前需要用户二次确认。当用户说【退货】【我要退货】【申请退货】时调用此工具。")
    public String returnOrder(
            @P(value = "订单编号") String orderNo) {

        if (orderNo == null || orderNo.isEmpty()) {
            return "🤔 请提供要退货的订单编号～";
        }

        return orderService.returnOrder(orderNo);
    }

    // ==================== 10. 售后问答（RAG） ====================

    @Tool(name = "askAfterSale",
            value = "回答用户关于售后政策的问题，如退换货规则、保修期限、退款流程等。" +
                    "当用户问到'可以退吗''退换货''保修''退款''售后'等关键词时调用此工具。" +
                    "回答将从知识库中检索相关信息后生成，确保回答准确。")
    public String askAfterSale(
            @P(value = "用户的售后问题") String question) {

        // 注意：RAG检索由 @AiService 中的 contentRetriever 自动完成
        // 此Tool返回引导信息，实际回答由AI结合RAG检索结果生成
        if (question == null || question.isEmpty()) {
            return "🤔 请告诉我您遇到的售后问题，我会尽力帮您解答～";
        }

        return "📋 正在为您检索「" + question + "」相关的售后政策...\n" +
                "（注：具体回答由AI结合知识库检索结果生成，请参考后续回复。）";
    }

    // ==================== 格式化辅助方法 ====================

    private String formatProductList(String title, List<Product> products) {
        return title + "\n\n" + formatProductListBody(products);
    }

    private String formatProductListBody(List<Product> products) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < products.size() && i < 10; i++) {
            Product p = products.get(i);
            sb.append(i + 1).append(". **").append(p.getName()).append("**\n");
            sb.append("   🆔 商品ID：").append(p.getId());
            sb.append(" | 🏷 品牌：").append(p.getBrand());
            sb.append(" | 💰 ¥").append(p.getPrice());
            sb.append(" | ⭐ ").append(p.getRating());
            sb.append(" | 📈 销量").append(p.getSales()).append("+\n");
            sb.append("   📦 平台：").append(p.getPlatform());
            sb.append(" | 📦 库存：").append(p.getStock()).append("件");
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                sb.append(" | 📝 ").append(p.getDescription());
            }
            sb.append("\n\n");
        }

        if (products.size() > 10) {
            sb.append("... 还有 ").append(products.size() - 10).append(" 个结果。");
        }

        return sb.toString();
    }

    private String formatSingleOrder(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 **订单详情**\n\n");
        sb.append("🔢 订单编号：**").append(order.getOrderNo()).append("**\n");
        sb.append("🛍 商品： ").append(order.getProductName()).append("\n");
        sb.append("💰 单价： ¥").append(order.getPrice()).append("\n");
        sb.append("🔢 数量： ").append(order.getQuantity()).append("\n");
        sb.append("💵 总金额： ¥").append(order.getTotalAmount()).append("\n");
        sb.append("👤 收货人： ").append(order.getRecipient()).append("\n");
        sb.append("📍 地址： ").append(order.getAddress()).append("\n");
        sb.append("📞 电话： ").append(order.getPhone()).append("\n");
        sb.append("📌 状态： **").append(order.getStatus()).append("**\n");
        sb.append("🕐 更新时间： ").append(order.getUpdateTime()).append("\n\n");

        // 根据状态给出不同的提示
        switch (order.getStatus()) {
            case "待付款":
                sb.append("💡 当前订单尚未付款。你可以回复 **'确认付款'** 进行模拟付款，或回复 **'取消订单'** 取消该订单。\n");
                sb.append("ℹ️ 说明：本系统为课程演示版，不提供真实付款链接。");
                break;
            case "已付款":
                sb.append("💡 订单已付款，等待商家模拟发货。你可以回复 **'取消订单'** 取消该订单。\n");
                sb.append("ℹ️ 说明：如需查询配送状态，可使用订单状态查询，当前系统未接入真实物流API。");
                break;
            case "已发货":
                sb.append("💡 订单已发货（系统模拟配送状态）。\n");
                sb.append("   你可以回复：**确认收货** / **退货**\n");
                sb.append("ℹ️ 说明：当前系统未接入真实物流平台，不提供实时物流轨迹和快递单号。");
                break;
            case "已完成":
                sb.append("💡 订单已完成，不可退货。感谢您的购买！🎉");
                break;
            case "已取消":
                sb.append("💡 该订单已取消。");
                break;
            case "已退货":
                sb.append("💡 该订单已退货，库存和销量已恢复。");
                break;
            default:
                break;
        }

        return sb.toString();
    }

    private String formatOrderList(List<Order> orders) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 您的订单列表（共 ").append(orders.size()).append(" 笔）：\n\n");

        for (int i = 0; i < orders.size() && i < 10; i++) {
            Order o = orders.get(i);
            sb.append(i + 1).append(". 📦 ").append(o.getProductName())
                    .append(" | 💵 ¥").append(o.getTotalAmount())
                    .append(" | 📌 **").append(o.getStatus()).append("**\n");
            sb.append("   订单号：").append(o.getOrderNo())
                    .append(" | 🕐 ").append(o.getUpdateTime()).append("\n\n");
        }

        if (orders.size() > 10) {
            sb.append("... 还有 ").append(orders.size() - 10).append(" 笔订单。");
        }

        return sb.toString();
    }
}
