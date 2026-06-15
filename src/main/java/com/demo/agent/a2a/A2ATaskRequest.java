package com.demo.agent.a2a;

import java.util.Map;

public record A2ATaskRequest(
        String taskId,
        String agentType,
        String name,
        String description,
        Map<String, String> upstream,
        String sessionId
) {}
