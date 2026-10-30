package com.xxx.proj.controller;

import com.xxx.proj.ai.ShopAgent;
import com.xxx.proj.vo.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "小智智能电商导购")
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopAgent shopAgent;

    @Operation(summary = "电商导购对话")
    @PostMapping("/chat")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        Long memoryId = chatForm.getMemoryId() != null ? chatForm.getMemoryId() : 1L;
        String userId = chatForm.getUserId() != null ? chatForm.getUserId() : "user-" + memoryId;
        return shopAgent.chat(memoryId, chatForm.getMessage(), userId);
    }
}
