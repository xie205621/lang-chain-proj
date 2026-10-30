package com.xxx.proj.store;

import com.xxx.proj.entity.ChatMessages;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MongoChatMemoryStore implements ChatMemoryStore {
    @Autowired
    private MongoTemplate mongoTemplate;
// 根据memoryId查询消息
// 读 MongoDB 反序列化 JSON
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        // 2. 从 MongoDB 查询出对应的文档实体 
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);
        // 3. 判空防NPE（应对新用户）
        if(chatMessages == null){ return new LinkedList<>();
        }

        List<ChatMessage> messages;
        try {
            // 4. 将查到的 JSON 字符串反序列化为 Java 的 List 对象
            messages = ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
        } catch (Exception e) {
            // JSON 损坏时兜底：清空该会话记忆，返回空列表，下次对话重新开始
            deleteMessages(memoryId);
            return new LinkedList<>();
        }

        // 5. 过滤不完整的 tool_calls 序列（LangChain4j beta3 bugfix — 双向过滤）
        //    5a. 收集 AiMessage 中所有请求的 tool_call_id
        Set<String> requestedToolIds = messages.stream()
                .filter(m -> m instanceof AiMessage aiMsg && aiMsg.hasToolExecutionRequests())
                .flatMap(m -> ((AiMessage) m).toolExecutionRequests().stream())
                .map(req -> req.id())
                .collect(Collectors.toSet());

        //    5b. 收集 ToolExecutionResultMessage 中所有返回的 tool_call_id
        Set<String> completedToolIds = messages.stream()
                .filter(m -> m instanceof ToolExecutionResultMessage)
                .map(m -> ((ToolExecutionResultMessage) m).id())
                .collect(Collectors.toSet());

        //    5c. 双向清理：去掉无法配对的消息
        messages.removeIf(m -> {
            // 孤儿 AiMessage：有 tool_calls 但没有对应的 tool 结果
            if (m instanceof AiMessage aiMsg && aiMsg.hasToolExecutionRequests()) {
                return aiMsg.toolExecutionRequests().stream()
                        .anyMatch(req -> !completedToolIds.contains(req.id()));
            }
            // 孤儿 ToolExecutionResultMessage：有 tool 结果但没有对应的 tool_calls
            if (m instanceof ToolExecutionResultMessage toolMsg) {
                return !requestedToolIds.contains(toolMsg.id());
            }
            return false;
        });

        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        Update update = new Update();
        // 把装满最新上下文的 List 序列化回 JSON 字符串
        update.set("content", ChatMessageSerializer.messagesToJson(messages));
        // upsert = update + insert：有就更新，没有就插入
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}