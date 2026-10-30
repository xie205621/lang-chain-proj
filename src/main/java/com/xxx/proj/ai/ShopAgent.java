package com.xxx.proj.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;
// Java 的动态代理技术
// 代理类
@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "openAiStreamingChatModel",
        //← 关键：指定记忆提供者
        chatMemoryProvider = "chatMemoryProviderShop",
        tools = "shopTools",
        contentRetriever = "contentRetrieverShop"
)
public interface ShopAgent {
// 第 1 层系统提示词 -> 第 2 层 MongoDB 历史记忆 -> 第 3 层最新消息
    @SystemMessage(fromResource = "shop-prompt-template.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage, @V("userId") String userId);
}
