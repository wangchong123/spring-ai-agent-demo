package com.demo.agent.chat;

public record ChatRequest(
        String sessionId,
        String message,
        String model
) {}
