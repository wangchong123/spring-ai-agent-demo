package com.demo.agent.orchestrator;

import com.demo.agent.chat.ChatClientFactory;
import com.demo.agent.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceSubAgent implements SubAgent {

    private final ChatClientFactory factory;
    private final ToolRegistry tools;

    @Override public String type() { return "finance"; }

    @Override
    public String run(SubTask task, Map<String, String> upstream, String sessionId) {
        String upstreamCtx = upstream.entrySet().stream()
                .map(e -> "[%s]\n%s".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n\n"));
        try {
            return factory.client("deepseek-pro").prompt()
                    .system("""
                        你是财务分析专家。基于上游任务结果与可用工具，完成本任务。
                        允许工具：queryReport / compareReport。
                        输出 JSON：{ summary, metrics[], risks[] }，确保 JSON 合法。
                        """)
                    .user("""
                        任务：%s
                        描述：%s
                        上游结果：
                        %s
                        """.formatted(task.name(), task.description(), upstreamCtx))
                    .toolCallbacks(tools.pick(List.of("queryReport", "compareReport")))
                    .call().content();
        } catch (Exception e) {
            log.warn("FinanceSubAgent.run failed: {}", e.getMessage());
            return "{\"error\":\"finance subagent failed: " + e.getMessage() + "\"}";
        }
    }
}
