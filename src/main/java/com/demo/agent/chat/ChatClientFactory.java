package com.demo.agent.chat;

import com.demo.agent.model.ModelRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatClientFactory {

    private final ModelRegistry registry;
    private final Map<String, ChatClient> cache = new ConcurrentHashMap<>();

    @Autowired @Lazy
    private ChatMemory chatMemory;

    public ChatClient client(String modelKey) {
        return cache.computeIfAbsent(modelKey, this::build);
    }

    private ChatClient build(String key) {
        ChatClient.Builder b = ChatClient.builder(registry.get(key))
                .defaultSystem("你是一个严谨、可靠的中文 AI 助理。")
                .defaultAdvisors(new SimpleLoggerAdvisor());
        if (chatMemory != null) {
            b = b.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        return b.build();
    }
}
