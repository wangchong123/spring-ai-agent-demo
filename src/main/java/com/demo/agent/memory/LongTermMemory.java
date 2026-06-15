package com.demo.agent.memory;

import com.demo.agent.chat.ChatClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 长期记忆。简化版：用 Redis Set 存事实；当 PgVector + EmbeddingModel 可用时可平替。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LongTermMemory {

    private final StringRedisTemplate redis;
    private final ChatClientFactory factory;

    @Async
    public void extractAsync(String userId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;
        String history = messages.stream()
                .map(m -> m.getMessageType().getValue() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
        String facts;
        try {
            facts = factory.client("qwen-flash").prompt()
                    .system("""
                        从对话中抽取值得长期记忆的用户事实，每行一条，格式：
                        [类别] 事实
                        类别仅限：身份/职业/偏好/约束/重要事件
                        没有则输出 NONE
                        """)
                    .user(history)
                    .call().content();
        } catch (Exception e) {
            log.warn("extract long-term facts failed: {}", e.getMessage());
            return;
        }
        if (facts == null || "NONE".equalsIgnoreCase(facts.trim())) return;

        String key = "mem:l3:" + userId;
        Arrays.stream(facts.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(s -> redis.opsForSet().add(key, s + " @" + Instant.now()));
    }

    public List<Message> recall(String userId, String query) {
        if (userId == null) return List.of();
        String key = "mem:l3:" + userId;
        Set<String> all = redis.opsForSet().members(key);
        if (all == null || all.isEmpty()) return List.of();

        // 简化：朴素关键词召回 top-3
        List<Message> out = new ArrayList<>();
        all.stream()
                .filter(f -> query == null || query.isBlank() || matchesAnyToken(f, query))
                .limit(3)
                .forEach(f -> out.add(new SystemMessage("[长期记忆] " + f)));
        return out;
    }

    private boolean matchesAnyToken(String fact, String query) {
        if (query.length() < 2) return false;
        for (int i = 0; i + 2 <= query.length(); i++) {
            if (fact.contains(query.substring(i, i + 2))) return true;
        }
        return false;
    }

    public String addFact(String userId, String fact) {
        String id = UUID.randomUUID().toString();
        redis.opsForSet().add("mem:l3:" + userId, fact);
        return id;
    }
}
