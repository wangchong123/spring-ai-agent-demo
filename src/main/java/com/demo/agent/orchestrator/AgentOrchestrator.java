package com.demo.agent.orchestrator;

import com.demo.agent.chat.ChatRequest;
import com.demo.agent.chat.ChatService;
import com.demo.agent.chat.SimpleChatResponse;
import com.demo.agent.model.router.ModelRouter;
import com.demo.agent.model.router.RoutingContext;
import com.demo.agent.skill.Skill;
import com.demo.agent.skill.SkillExecutor;
import com.demo.agent.skill.SkillRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

/**
 * 顶层编排：
 *   1) 意图识别
 *   2) Skill 命中？走 SkillExecutor
 *   3) 复杂任务？拆 DAG → SubAgent 并发执行 → Reducer 合成
 *   4) 简单任务？直接走 ChatService.simpleStream
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final IntentClassifier intentClassifier;
    private final SkillRouter skillRouter;
    private final SkillExecutor skillExecutor;
    private final SubAgentExecutor subAgentExecutor;
    private final ResultReducer reducer;
    private final ModelRouter router;
    private final ChatService chat;

    public SimpleChatResponse runSync(ChatRequest req, RoutingContext ctx) {
        String all = runStream(req, ctx).collectList().block().stream()
                .reduce("", String::concat);
        return new SimpleChatResponse(all, router.pickModel(ctx), req.sessionId());
    }

    public Flux<String> runStream(ChatRequest req, RoutingContext ctxIn) {
        IntentResult intent = intentClassifier.classify(req.message());
        RoutingContext ctx = ctxIn.withIntent(intent.intent(), intent.complexity());
        String modelKey = router.pickModel(ctx);
        log.info("[orchestrate] sid={} intent={} complexity={} model={}",
                req.sessionId(), intent.intent(), intent.complexity(), modelKey);

        // 1) Skill 优先
        Optional<Skill> skill = skillRouter.route(req.message());
        if (skill.isPresent()) {
            log.info("[orchestrate] skill matched: {}", skill.get().name());
            return Flux.concat(
                    Flux.just("[Skill: " + skill.get().name() + "]\n"),
                    skillExecutor.stream(skill.get(), req.message(), modelKey, req.sessionId())
            );
        }

        // 2) 复杂任务：拆 DAG
        if (intent.complexity() == Complexity.COMPLEX && !intent.subtasks().isEmpty()) {
            log.info("[orchestrate] dispatching {} subtasks", intent.subtasks().size());
            try {
                TaskDag dag = new TaskDag(intent.subtasks());
                Map<String, String> sub = subAgentExecutor.execute(dag, req.sessionId());
                String banner = "[已拆解为 " + intent.subtasks().size() + " 个子任务并执行完毕，开始合成结果]\n\n";
                return Flux.concat(
                        Flux.just(banner),
                        reducer.reduce(req.message(), sub, modelKey, req.sessionId())
                );
            } catch (Exception e) {
                log.warn("orchestrate complex failed, fallback to simple: {}", e.getMessage());
            }
        }

        // 3) 简单任务
        return chat.simpleStream(modelKey, req);
    }
}
