package com.demo.agent.chat;

public record SimpleChatResponse(
        String content,
        String model,
        String sessionId
) {}
