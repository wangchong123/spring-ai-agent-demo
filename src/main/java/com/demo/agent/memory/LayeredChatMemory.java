package com.demo.agent.memory;

import com.demo.agent.config.AgentProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 三层记忆：
 *  L1 工作记忆（Redis List）—— 最近 N 轮原始对话
 *  L2 会话摘要（Redis String）—— 滚动压缩
 *  L3 长期记忆（PgVector / 简化版用 Redis Set）—— 用户长期事实
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LayeredChatMemory implements ChatMemory {

    private final StringRedisTemplate redis;
    private final AgentProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired @Lazy
    private SummaryCompressor compressor;

    @Autowired @Lazy
    private LongTermMemory longTerm;

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = "mem:l1:" + conversationId;
        try {
            for (Message m : messages) {
                redis.opsForList().rightPush(key, serialize(m));
            }
            Long size = redis.opsForList().size(key);
            if (size != null && size > props.getMemory().getL1MaxMessages()) {
                try {
                    compressIfNeeded(conversationId);
                } catch (Exception e) {
                    log.warn("compress failed, ignored: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("memory.add skipped (redis unavailable?): {}", e.getMessage());
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        String key = "mem:l1:" + conversationId;
        List<Message> result = new ArrayList<>();
        List<String> raw = null;
        try {
            raw = redis.opsForList().range(key, 0, -1);
            String summary = redis.opsForValue().get("mem:l2:" + conversationId);
            if (summary != null && !summary.isBlank()) {
                result.add(new SystemMessage("[历史摘要] " + summary));
            }
        } catch (Exception e) {
            log.debug("memory.get fell back to empty (redis unavailable?): {}", e.getMessage());
            return result;
        }
        try {
            String lastUser = lastUserMessage(raw);
            result.addAll(longTerm.recall(conversationId, lastUser));
        } catch (Exception e) {
            log.debug("longTerm.recall skipped: {}", e.getMessage());
        }
        if (raw != null) {
            for (String s : raw) {
                Message m = deserialize(s);
                if (m != null) result.add(m);
            }
        }
        return result;
    }

    @Override
    public void clear(String conversationId) {
        try {
            redis.delete(List.of(
                    "mem:l1:" + conversationId,
                    "mem:l2:" + conversationId
            ));
        } catch (Exception e) {
            log.debug("memory.clear skipped: {}", e.getMessage());
        }
    }

    private void compressIfNeeded(String conversationId) {
        String key = "mem:l1:" + conversationId;
        int batch = props.getMemory().getCompressBatchSize();
        List<String> oldest = redis.opsForList().range(key, 0, batch - 1);
        if (oldest == null || oldest.isEmpty()) return;

        List<Message> batchMsgs = oldest.stream().map(this::deserialize).filter(java.util.Objects::nonNull).toList();
        String newSummary = compressor.summarize(batchMsgs);
        String prev = redis.opsForValue().get("mem:l2:" + conversationId);
        String merged = (prev == null || prev.isBlank()) ? newSummary : compressor.merge(prev, newSummary);
        redis.opsForValue().set("mem:l2:" + conversationId, merged);
        redis.opsForList().trim(key, batch, -1);

        longTerm.extractAsync(conversationId, batchMsgs);
    }

    private String serialize(Message m) {
        try {
            return mapper.writeValueAsString(Map.of(
                    "type", m.getMessageType().getValue(),
                    "text", m.getText() == null ? "" : m.getText()));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Message deserialize(String json) {
        try {
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String type = String.valueOf(map.get("type"));
            String text = String.valueOf(map.getOrDefault("text", ""));
            MessageType mt = MessageType.fromValue(type);
            return switch (mt) {
                case USER -> new UserMessage(text);
                case ASSISTANT -> new AssistantMessage(text);
                case SYSTEM -> new SystemMessage(text);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String lastUserMessage(List<String> raw) {
        if (raw == null) return null;
        for (int i = raw.size() - 1; i >= 0; i--) {
            Message m = deserialize(raw.get(i));
            if (m instanceof UserMessage) return m.getText();
        }
        return null;
    }
}
