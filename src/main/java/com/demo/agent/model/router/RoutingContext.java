package com.demo.agent.model.router;

import com.demo.agent.orchestrator.Complexity;
import com.demo.agent.orchestrator.Intent;

public record RoutingContext(
        String sessionId,
        String userMessage,
        String userPreferredModel,
        Intent intent,
        Complexity complexity,
        int tokenEstimate
) {
    public static RoutingContext of(String sessionId, String message, String preferred) {
        return new RoutingContext(sessionId, message,
                (preferred == null || preferred.isBlank() || "auto".equalsIgnoreCase(preferred)) ? null : preferred,
                null, null, message == null ? 0 : message.length() / 2);
    }

    public RoutingContext withIntent(Intent intent, Complexity complexity) {
        return new RoutingContext(sessionId, userMessage, userPreferredModel,
                intent, complexity, tokenEstimate);
    }
}
