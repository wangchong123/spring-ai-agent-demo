package com.demo.agent.orchestrator;

import java.util.List;

public record SubTask(
        String id,
        String name,
        String description,
        String agentType,
        List<String> dependsOn
) {}
