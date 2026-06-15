package com.demo.agent.chat;

import com.demo.agent.memory.LayeredChatMemory;
import com.demo.agent.model.cost.CostTracker;
import com.demo.agent.model.router.ModelRouter;
import com.demo.agent.model.router.RoutingContext;
import com.demo.agent.orchestrator.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClientFactory factory;
    private final ModelRouter router;
    private final CostTracker costTracker;
    private final LayeredChatMemory memory;

    @Autowired @Lazy
    private AgentOrchestrator orchestrator;

    public SimpleChatResponse chat(ChatRequest req) {
        RoutingContext ctx = RoutingContext.of(req.sessionId(), req.message(), req.model());
        // 让 orchestrator 决定走 simple / orchestrated 路径
        return orchestrator.runSync(req, ctx);
    }

    public Flux<String> stream(ChatRequest req) {
        RoutingContext ctx = RoutingContext.of(req.sessionId(), req.message(), req.model());
        return orchestrator.runStream(req, ctx);
    }

    /** 简单纯流式调用：纯模型 + 记忆，不做意图识别/Skill/RAG，给 orchestrator 兜底用。 */
    public Flux<String> simpleStream(String modelKey, ChatRequest req) {
        return factory.client(modelKey).prompt()
                .user(req.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, req.sessionId()))
                .stream()
                .content()
                .doOnComplete(() -> costTracker.record(modelKey, req.sessionId(),
                        estimateTokens(req.message()), 0))
                .onErrorResume(ex -> {
                    String fb = router.fallback(modelKey);
                    log.warn("model {} failed, fallback to {}: {}", modelKey, fb, ex.getMessage());
                    return factory.client(fb).prompt()
                            .user(req.message())
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, req.sessionId()))
                            .stream().content();
                });
    }

    /** 同步调用，用于 orchestrator / SubAgent 内部。 */
    public String simpleCall(String modelKey, String system, String user) {
        try {
            return factory.client(modelKey).prompt()
                    .system(system)
                    .user(user)
                    .call().content();
        } catch (Exception ex) {
            String fb = router.fallback(modelKey);
            log.warn("call {} failed, fallback to {}: {}", modelKey, fb, ex.getMessage());
            return factory.client(fb).prompt().system(system).user(user).call().content();
        }
    }

    private int estimateTokens(String s) {
        return s == null ? 0 : Math.max(1, s.length() / 2);
    }
}
