package com.demo.agent.orchestrator;

import com.demo.agent.a2a.A2AClient;
import com.demo.agent.a2a.AgentCard;
import com.demo.agent.a2a.A2ARegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubAgentExecutor {

    private final List<SubAgent> localAgents;
    private final GenericSubAgent generic;
    private final A2ARegistry a2aRegistry;
    private final A2AClient a2aClient;

    public Map<String, String> execute(TaskDag dag, String sessionId) {
        Map<String, String> results = new ConcurrentHashMap<>();
        for (List<SubTask> layer : dag.topoLayers()) {
            executeLayer(layer, results, sessionId);
        }
        return results;
    }

    @SuppressWarnings("preview")
    private void executeLayer(List<SubTask> layer, Map<String, String> results, String sessionId) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Map<SubTask, StructuredTaskScope.Subtask<String>> subs = new HashMap<>();
            for (SubTask t : layer) {
                subs.put(t, scope.fork(() -> dispatch(t, results, sessionId)));
            }
            scope.join();
            scope.throwIfFailed();
            subs.forEach((t, s) -> results.put(t.id(), s.get()));
        } catch (Exception e) {
            log.error("Layer execution failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String dispatch(SubTask t, Map<String, String> upstream, String sessionId) {
        // 1) 本地 SubAgent
        for (SubAgent a : localAgents) {
            if (a.type().equalsIgnoreCase(t.agentType())) {
                log.info("Dispatch [{}] to local SubAgent [{}]", t.id(), a.type());
                return a.run(t, upstream, sessionId);
            }
        }
        // 2) A2A 远程 Agent
        Optional<AgentCard> peer = a2aRegistry.find(t.agentType());
        if (peer.isPresent()) {
            log.info("Dispatch [{}] to A2A peer [{}]", t.id(), peer.get().name());
            try {
                return a2aClient.invoke(peer.get(), t, upstream);
            } catch (Exception e) {
                log.warn("A2A invoke failed, fallback to generic: {}", e.getMessage());
            }
        }
        // 3) 兜底：通用 SubAgent
        log.info("Dispatch [{}] to GenericSubAgent (no specialized handler)", t.id());
        return generic.run(t, upstream, sessionId);
    }
}
