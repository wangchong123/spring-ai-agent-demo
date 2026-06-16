package com.demo.agent.orchestrator;

import com.demo.agent.chat.ChatClientFactory;
import com.demo.agent.model.ModelRegistry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 意图识别 + 复杂度评估 + 子任务拆解。
 * 实现方式：让 LLM 用结构化输出（Spring AI 的 .entity(Class)）直接吐出可解析对象。
 *
 * 当 ModelRegistry 全部为 mock（即没有任何真实 API Key），LLM 路径会返回 mock 文本无法解析；
 * 此时降级到一个最简启发式分类器，确保 mock 模式下整条链路仍可演示 DAG 拆解。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {

    private final ChatClientFactory factory;
    private final ModelRegistry registry;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LlmIntentOutput(
            String intent,
            String complexity,
            String recommendedModel,
            List<SubTaskDto> subtasks
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SubTaskDto(String id, String name, String description,
                                 String agentType, List<String> dependsOn) {}
    }

    public IntentResult classify(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return IntentResult.simple();

        // mock 模式（所有模型都是 MockChatModel）下走启发式，避免拿 mock 文本去解析
        if (allMock()) {
            log.debug("[intent] all models are mock, using heuristic classifier");
            return HeuristicIntent.classify(userMessage);
        }

        try {
            LlmIntentOutput out = factory.client("deepseek-flash")
                    .prompt()
                    .system("""
                        你是一个意图分类器。分析用户请求，输出 JSON：
                        {
                          "intent": "CHITCHAT|QA|RAG|CODE|ANALYSIS|ACTION|SUMMARIZE",
                          "complexity": "SIMPLE|MEDIUM|COMPLEX",
                          "recommendedModel": "deepseek-pro|deepseek-flash|qwen-max|qwen-plus|qwen-flash|glm-5|glm-flash",
                          "subtasks": [
                            { "id":"t1","name":"...","description":"...",
                              "agentType":"pdf|finance|mail|code|web|sql|generic",
                              "dependsOn":[] }
                          ]
                        }
                        规则：
                          - SIMPLE：单步即可（闲聊、定义解释、单点问答）。subtasks 留空数组。
                          - MEDIUM：1-2 步、可能需 1 个工具。subtasks 留空数组。
                          - COMPLEX：≥3 步或跨数据源。必须输出 subtasks（含 DAG 依赖）。
                        只输出 JSON，不要任何额外文字。
                        """)
                    .user(userMessage)
                    .call()
                    .entity(LlmIntentOutput.class);
            return toResult(out);
        } catch (Exception e) {
            log.warn("[intent] LLM classification failed, fallback to heuristic: {}", e.getMessage());
            return HeuristicIntent.classify(userMessage);
        }
    }

    private boolean allMock() {
        return registry.all().values().stream().allMatch(d -> d.mock());
    }

    private IntentResult toResult(LlmIntentOutput out) {
        if (out == null) return IntentResult.simple();
        Intent intent = parseEnum(Intent.class, out.intent(), Intent.QA);
        Complexity c = parseEnum(Complexity.class, out.complexity(), Complexity.SIMPLE);
        List<SubTask> subtasks = (out.subtasks() == null) ? List.of() :
                out.subtasks().stream()
                        .filter(d -> d.id() != null && d.agentType() != null)
                        .map(d -> new SubTask(
                                d.id(), nullSafe(d.name()), nullSafe(d.description()),
                                d.agentType(),
                                d.dependsOn() == null ? List.of() : d.dependsOn()))
                        .toList();
        return new IntentResult(intent, c, out.recommendedModel(), subtasks);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private static <E extends Enum<E>> E parseEnum(Class<E> cls, String v, E fallback) {
        if (v == null) return fallback;
        try { return Enum.valueOf(cls, v.trim().toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
}
