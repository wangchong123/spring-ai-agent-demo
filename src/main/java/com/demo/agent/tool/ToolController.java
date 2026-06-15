package com.demo.agent.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@CrossOrigin
public class ToolController {

    private final ToolRegistry registry;

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, String>> tools = registry.all().stream()
                .map(t -> Map.of(
                        "name", t.getToolDefinition().name(),
                        "description", t.getToolDefinition().description()))
                .toList();
        return Map.of("count", tools.size(), "tools", tools);
    }
}
