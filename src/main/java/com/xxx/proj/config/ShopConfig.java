package com.xxx.proj.config;

import com.xxx.proj.store.MongoChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ShopConfig {

    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    // 为每个用户创建独立记忆
    @Bean
    ChatMemoryProvider chatMemoryProviderShop() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }

    @Bean
    ContentRetriever contentRetrieverShop() {
        Document document1 = FileSystemDocumentLoader.loadDocument("knowledgenew/商品指南.md");
        Document document2 = FileSystemDocumentLoader.loadDocument("knowledgenew/售后政策.md");
        Document document3 = FileSystemDocumentLoader.loadDocument("knowledgenew/促销活动.md");
        List<Document> documents = Arrays.asList(document1, document2, document3);

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}
