package com.demo.agent.model;

import com.demo.agent.config.AgentProperties;
import com.demo.agent.mock.MockChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRegistry {

    private final AgentProperties props;
    private final Map<String, ChatModel> models = new ConcurrentHashMap<>();
    private final Map<String, ModelDescriptor> descriptors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        props.getModels().forEach((key, cfg) -> {
            boolean hasKey = cfg.getApiKey() != null && !cfg.getApiKey().isBlank();
            ChatModel cm;
            boolean isMock;
            if (!hasKey && props.isMockWhenNoApiKey()) {
                cm = new MockChatModel(key);
                isMock = true;
                log.warn("Model [{}] has no API key, using MockChatModel.", key);
            } else if (!hasKey) {
                throw new IllegalStateException("Model " + key + " has no API key, and mock fallback is disabled.");
            } else {
                OpenAiApi api = OpenAiApi.builder()
                        .baseUrl(cfg.getBaseUrl())
                        .apiKey(cfg.getApiKey())
                        .build();
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .temperature(0.3)
                        .build();
                cm = OpenAiChatModel.builder()
                        .openAiApi(api)
                        .defaultOptions(options)
                        .build();
                isMock = false;
                log.info("Model [{}] registered: provider={}, model={}", key, cfg.getProvider(), cfg.getModel());
            }
            models.put(key, cm);
            descriptors.put(key, new ModelDescriptor(
                    key, cfg.getProvider(), cfg.getModel(), cfg.getBaseUrl(),
                    cfg.getContextWindow(), cfg.getInputPrice(), cfg.getOutputPrice(),
                    isMock));
        });
        if (models.isEmpty()) {
            throw new IllegalStateException("No model registered. Check agent.models configuration.");
        }
        log.info("ModelRegistry initialized with {} models: {}", models.size(), models.keySet());
    }

    public ChatModel get(String key) {
        ChatModel m = models.get(key);
        if (m == null) {
            throw new IllegalArgumentException("Unknown model key: " + key + ", available: " + models.keySet());
        }
        return m;
    }

    public ModelDescriptor descriptor(String key) {
        return descriptors.get(key);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(models.keySet());
    }

    public Map<String, ModelDescriptor> all() {
        return Collections.unmodifiableMap(descriptors);
    }
}
