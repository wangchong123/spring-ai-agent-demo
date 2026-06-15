package com.demo.agent.a2a;

import java.util.List;
import java.util.Map;

public record AgentCard(
        String name,
        String description,
        String url,
        String version,
        Map<String, Object> capabilities,
        List<SkillSpec> skills,
        Map<String, Object> authentication
) {
    public record SkillSpec(String id, String name, String description,
                            List<String> inputModes, List<String> outputModes) {}

    public static AgentCard self(String name, String description, String url) {
        return new AgentCard(
                name, description, url, "1.0.0",
                Map.of("streaming", true, "pushNotifications", false),
                List.of(new SkillSpec("default", name + " 默认能力", description,
                        List.of("text"), List.of("text"))),
                Map.of("schemes", List.of("none"))
        );
    }
}
