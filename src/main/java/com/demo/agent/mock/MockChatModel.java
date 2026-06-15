package com.demo.agent.mock;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * 没有真实 API Key 时的兜底实现。
 * 让骨架可以 mvn spring-boot:run 起来，演示框架能力。
 */
public class MockChatModel implements ChatModel {

    private final String modelName;

    public MockChatModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String userMsg = prompt.getInstructions().stream()
                .reduce((a, b) -> b)
                .map(m -> m.getText())
                .orElse("");
        String reply = "[mock-" + modelName + "] 已收到 " + userMsg.length() + " 字符的输入。请配置真实 API Key 以使用 " + modelName + "。";
        return new ChatResponse(List.of(new Generation(new AssistantMessage(reply),
                ChatGenerationMetadata.NULL)));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String userMsg = prompt.getInstructions().stream()
                .reduce((a, b) -> b)
                .map(m -> m.getText())
                .orElse("");
        String reply = "[mock-" + modelName + "] 流式回应：你说了 " + userMsg.length()
                + " 个字符。配置真实 API Key 即可调用 " + modelName + "。";
        String[] tokens = reply.split("(?<=\\G.{4})");
        return Flux.fromArray(tokens)
                .delayElements(Duration.ofMillis(60))
                .map(t -> new ChatResponse(List.of(new Generation(new AssistantMessage(t),
                        ChatGenerationMetadata.NULL))));
    }
}
