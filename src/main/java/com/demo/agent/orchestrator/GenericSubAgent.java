package com.demo.agent.orchestrator;

import com.demo.agent.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GenericSubAgent implements SubAgent {

    private final ChatService chat;

    @Override public String type() { return "generic"; }

    @Override
    public String run(SubTask task, Map<String, String> upstream, String sessionId) {
        String upstreamCtx = upstream.entrySet().stream()
                .map(e -> "[%s] %s".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
        return chat.simpleCall("deepseek-flash",
                "你是一个 SubAgent，专注于完成本任务并返回结果文本。",
                """
                任务：%s
                描述：%s
                上游：
                %s
                """.formatted(task.name(), task.description(), upstreamCtx));
    }
}
