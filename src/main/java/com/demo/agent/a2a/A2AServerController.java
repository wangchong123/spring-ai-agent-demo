package com.demo.agent.a2a;

import com.demo.agent.config.AgentProperties;
import com.demo.agent.orchestrator.SubAgent;
import com.demo.agent.orchestrator.SubTask;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A 服务端：
 *   - GET  /.well-known/agent.json    Agent Card 自描述
 *   - POST /a2a/tasks/send            同步执行任务（演示版；流式版可加 SSE）
 *
 * 默认把 main-agent 自身已注册的所有 SubAgent 暴露成 A2A，便于多实例互相调用。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class A2AServerController {

    private final List<SubAgent> localAgents;
    private final AgentProperties props;

    private AgentCard card;

    @PostConstruct
    public void init() {
        List<AgentCard.SkillSpec> skills = localAgents.stream()
                .map(a -> new AgentCard.SkillSpec(
                        a.type(),
                        a.type() + "-agent",
                        "本 Agent 暴露的 " + a.type() + " 能力",
                        List.of("text"),
                        List.of("text")))
                .toList();
        this.card = new AgentCard(
                "spring-ai-agent-demo",
                "Production-grade Java AI Agent Demo (multi-model + RAG + Tools + Skill + Orchestrator + A2A)",
                "http://localhost:" + props.getA2a().getSelfPort(),
                "1.0.0",
                Map.of("streaming", true, "pushNotifications", false),
                skills,
                Map.of("schemes", List.of("none"))
        );
    }

    @GetMapping("/.well-known/agent.json")
    public AgentCard card() {
        return card;
    }

    @PostMapping("/a2a/tasks/send")
    public A2ATaskResponse send(@RequestBody A2ATaskRequest req) {
        log.info("[a2a-server] received task: id={} type={} name={}", req.taskId(), req.agentType(), req.name());
        for (SubAgent a : localAgents) {
            if (a.type().equalsIgnoreCase(req.agentType())) {
                try {
                    SubTask t = new SubTask(req.taskId(), req.name(), req.description(),
                            req.agentType(), List.of());
                    Map<String, String> up = req.upstream() == null ? new HashMap<>() : req.upstream();
                    String result = a.run(t, up, req.sessionId());
                    return A2ATaskResponse.completed(req.taskId(), result);
                } catch (Exception e) {
                    return A2ATaskResponse.failed(req.taskId(), e.getMessage());
                }
            }
        }
        return A2ATaskResponse.failed(req.taskId(),
                "no SubAgent for agentType=" + req.agentType());
    }
}
