package com.demo.agent.orchestrator;

import java.util.Map;

public interface SubAgent {
    String type();
    String run(SubTask task, Map<String, String> upstream, String sessionId);
}
