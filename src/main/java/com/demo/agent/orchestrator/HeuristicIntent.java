package com.demo.agent.orchestrator;

import java.util.List;
import java.util.regex.Pattern;

/**
 * mock 模式下的兜底分类器。仅保留最基本的启发式，演示完整的 DAG 链路。
 * 真实生产请走 LLM 结构化输出（IntentClassifier 主路径）。
 */
final class HeuristicIntent {

    private static final Pattern COMPLEX_HINT = Pattern.compile(
            "(分析.*PDF|对比.*同期|起草.*邮件|生成.*方案|分析.*季报|分析.*年报)");
    private static final Pattern SIMPLE_HINT = Pattern.compile(
            "^(你好|hello|hi|嗨|在吗|谢谢|再见|是谁|你是).{0,8}$");

    private HeuristicIntent() {}

    static IntentResult classify(String userMessage) {
        String trimmed = userMessage.trim();
        if (SIMPLE_HINT.matcher(trimmed).find()) {
            return new IntentResult(Intent.CHITCHAT, Complexity.SIMPLE, null, List.of());
        }
        if (COMPLEX_HINT.matcher(trimmed).find()) {
            return new IntentResult(Intent.ANALYSIS, Complexity.COMPLEX, "deepseek-pro",
                    List.of(
                            new SubTask("t1", "读取数据", "读取用户提到的 PDF / 数据源",
                                    "pdf", List.of()),
                            new SubTask("t2", "提取关键指标", "提取财务三表数据",
                                    "finance", List.of("t1")),
                            new SubTask("t3", "对比同期", "调 queryReport / compareReport 做对比",
                                    "finance", List.of("t2")),
                            new SubTask("t4", "起草邮件", "生成投资简报邮件",
                                    "mail", List.of("t3"))
                    ));
        }
        if (trimmed.length() > 200) {
            return new IntentResult(Intent.ANALYSIS, Complexity.MEDIUM, null, List.of());
        }
        return new IntentResult(Intent.QA, Complexity.SIMPLE, null, List.of());
    }
}
