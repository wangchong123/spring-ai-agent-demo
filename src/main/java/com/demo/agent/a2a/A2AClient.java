package com.demo.agent.a2a;

import com.demo.agent.orchestrator.SubTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class A2AClient {

    private final WebClient web = WebClient.builder().build();

    public String invoke(AgentCard peer, SubTask task, Map<String, String> upstream) {
        A2ATaskRequest req = new A2ATaskRequest(
                task.id(), task.agentType(), task.name(), task.description(),
                upstream, "a2a-call");
        try {
            A2ATaskResponse resp = web.post()
                    .uri(peer.url() + "/a2a/tasks/send")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(A2ATaskResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            if (resp == null) throw new IllegalStateException("null A2A response");
            if ("FAILED".equals(resp.state())) {
                throw new IllegalStateException("A2A peer failed: " + resp.error());
            }
            return resp.result();
        } catch (Exception e) {
            log.warn("[a2a-client] invoke {} failed: {}", peer.name(), e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
