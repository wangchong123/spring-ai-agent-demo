package com.demo.agent.orchestrator;

import java.util.List;

public record IntentResult(
        Intent intent,
        Complexity complexity,
        String recommendedModel,
        List<SubTask> subtasks
) {
    public static IntentResult simple() {
        return new IntentResult(Intent.QA, Complexity.SIMPLE, null, List.of());
    }
}
