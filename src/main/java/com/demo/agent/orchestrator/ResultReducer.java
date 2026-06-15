package com.demo.agent.orchestrator;

import com.demo.agent.chat.ChatClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultReducer {

    private final ChatClientFactory factory;

    public Flux<String> reduce(String userMessage, Map<String, String> subResults,
                               String modelKey, String sessionId) {
        String parts = subResults.entrySet().stream()
                .map(e -> "## %s\n%s".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n\n"));
        return factory.client(modelKey).prompt()
                .system("""
                    你将整合多个子 Agent 的产出，给出对用户友好的最终回答。
                    要求：
                    1) 直接面向用户，不要暴露内部 SubAgent 名
                    2) 关键数据列表/对比用 Markdown 表格
                    3) 末尾给出 3 条可执行建议
                    """)
                .user("""
                    用户原问题：%s

                    子任务结果：
                    %s
                    """.formatted(userMessage, parts))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream().content();
    }
}
