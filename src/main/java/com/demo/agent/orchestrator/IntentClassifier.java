package com.demo.agent.orchestrator;

import com.demo.agent.chat.ChatClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图分类器：先用启发式快速判断（避免每条都打模型 + 在 mock 模式下也能工作），
 * 不命中则降级到 LLM 结构化输出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {

    private final ChatClientFactory factory;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern COMPLEX_HINT = Pattern.compile(
            "(分析.*PDF|对比.*同期|起草.*邮件|生成.*方案|分析.*季报|分析.*年报)");
    private static final Pattern SIMPLE_HINT = Pattern.compile(
            "^(你好|hello|hi|嗨|在吗|谢谢|再见|是谁|你是).{0,8}$");

    public IntentResult classify(String userMessage) {
        if (userMessage == null) return IntentResult.simple();
        String trimmed = userMessage.trim();

        if (SIMPLE_HINT.matcher(trimmed).find()) {
            return new IntentResult(Intent.CHITCHAT, Complexity.SIMPLE, null, List.of());
        }
        if (COMPLEX_HINT.matcher(trimmed).find()) {
            return planComplex(trimmed);
        }
        if (trimmed.length() > 200) {
            return new IntentResult(Intent.ANALYSIS, Complexity.MEDIUM, null, List.of());
        }
        return new IntentResult(Intent.QA, Complexity.SIMPLE, null, List.of());
    }

    private IntentResult planComplex(String userMessage) {
        // 真实生产用 .entity(IntentResult.class)；这里直接产出固定 DAG 演示
        // 让 demo 在 mock 模型下也能展示 DAG 拆解。
        List<SubTask> subtasks = List.of(
                new SubTask("t1", "读取数据", "读取用户提到的 PDF / 数据源",
                        "pdf", List.of()),
                new SubTask("t2", "提取关键指标", "提取财务三表数据",
                        "finance", List.of("t1")),
                new SubTask("t3", "对比同期", "调 queryReport / compareReport 做对比",
                        "finance", List.of("t2")),
                new SubTask("t4", "起草邮件", "生成投资简报邮件",
                        "mail", List.of("t3"))
        );
        return new IntentResult(Intent.ANALYSIS, Complexity.COMPLEX, "deepseek-pro", subtasks);
    }
}
