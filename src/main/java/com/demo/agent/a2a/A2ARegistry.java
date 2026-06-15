package com.demo.agent.a2a;

import com.demo.agent.config.AgentProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class A2ARegistry {

    private final AgentProperties props;
    private final WebClient web = WebClient.builder().build();
    private final ConcurrentMap<String, AgentCard> peers = new ConcurrentHashMap<>();

    @PostConstruct
    public void discover() {
        if (!props.getA2a().isEnabled()) return;
        List<String> urls = props.getA2a().getPeers();
        if (urls == null || urls.isEmpty()) {
            log.info("[a2a] no peers configured.");
            return;
        }
        for (String url : urls) {
            try {
                AgentCard c = web.get().uri(url + "/.well-known/agent.json")
                        .retrieve().bodyToMono(AgentCard.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                if (c != null) {
                    peers.put(c.name(), c);
                    log.info("[a2a] discovered peer: {} -> {}", c.name(), url);
                }
            } catch (Exception e) {
                log.warn("[a2a] discover {} failed: {}", url, e.getMessage());
            }
        }
    }

    /** 按 SubTask.agentType 匹配 peer：peer 暴露的 skill.id 与 agentType 相同则视为可处理 */
    public Optional<AgentCard> find(String agentType) {
        for (AgentCard c : peers.values()) {
            for (AgentCard.SkillSpec s : c.skills()) {
                if (s.id().equalsIgnoreCase(agentType)) return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public void register(AgentCard card) { peers.put(card.name(), card); }

    public java.util.Collection<AgentCard> all() { return peers.values(); }
}
